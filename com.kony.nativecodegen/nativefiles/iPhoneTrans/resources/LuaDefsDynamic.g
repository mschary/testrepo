tree grammar LuaDefsDynamic;

options {
  language = Java;
  output = AST;
  tokenVocab = Lua;
  ASTLabelType = LuaAST;
  filter = true;
  backtrack = true;
  k = 2;
}


@header {
  package com.konylabs.transformer.lua.grammer;
  import com.konylabs.transformer.scopes.*;
}


@members {
    SymbolTable symtab;
    Scope currentScope;
    MethodSymbol currentMethod;
    StructSymbol currentTable;
    public boolean hadErrors = false;
    Stack paraphrases = new Stack();
    String fileName = null;
    Type StringType = null;
    Type CallbackPtr = null;
    public StructSymbol globalApi = null;
    
    public LuaDefsDynamic(TreeNodeStream input, SymbolTable symtab, String fileName) {
        this(input);
        this.symtab = symtab;
        currentScope = symtab.fileScope;
        this.fileName = fileName;
        Type st = (Type) symtab.globals.resolve("String");
        if(st != null){
          StringType = st;
        }else{
          StringType = new BuiltInTypeSymbol("String",SymbolTable.tUSER);
          symtab.globals.define((Symbol) StringType);
        }
        st = (Type) symtab.globals.resolve("CallbackPtr");
        if(st != null){
          CallbackPtr = st;
        }else{
          CallbackPtr = new BuiltInTypeSymbol("CallbackPtr",SymbolTable.tUSER);
          symtab.globals.define((Symbol) CallbackPtr);
        }
        Symbol ss = symtab.globals.resolve("globalapi");
        if(ss != null){
            globalApi = (StructSymbol) ss.type;
        }
    }

    public Type getType(LuaAST rNode)
    {
        if (rNode == null)
        {
            return (SymbolTable._void);
        }
        
        if (rNode.getType() != DOT)
        {
            Type t = rNode.evalType;
            if (t == null)
            {
                Symbol rsym = currentScope.resolve(rNode.getText());
                if (rsym != null) 
                {
                    rNode.evalType = rsym.type;
                    rNode.type = rsym.type;
                    t = rsym.type;
                }                
            }
            return (t);
        }
        LuaAST tnode = null;
        LuaAST fnode = null;
        Symbol tsym = null;
        Symbol fsym = null;
        Scope scope = currentScope;
        if (rNode.getType() == DOT)
        {
            tnode = (LuaAST) rNode.getChild(0);
            tsym = scope.resolve(tnode.getText());
            if ((tsym == null) || !(tsym.type instanceof StructSymbol))
            {
                return (SymbolTable._void);
            }
            fnode = (LuaAST) rNode.getChild(1);
            scope = (Scope) tsym.type;
            fsym = scope.resolve(fnode.getText());
            if (fsym == null)
            {
               return (SymbolTable._void); 
            }
            return (fsym.type);
        }
        return (rNode.evalType);
    }

    Symbol getArrayField(String name, StructSymbol tSym, LuaAST start, LuaAST child){
        Symbol vSym = null;
        if(name.charAt(0) == '\"')
          name = name.substring(1,name.length()-1);
        vSym = tSym.resolveMember(name);
        if (vSym == null) {
//            child.getToken().setText(name);
            vSym = new VariableSymbol(name, SymbolTable._void);
            vSym.def = child;
            child.symbol = vSym;
            child.scope = tSym;
            tSym.define(vSym);
            start.isDeclaration = true;
            //System.out.println("New Array Symbol added " + vSym);
        }
        return vSym;
    }
    
    Symbol addNewField(java.util.Iterator itr, Type type)
    {
         Scope scope = currentScope;
         Symbol parent = null;
         Symbol child = null;
         String tname = "";
         LuaAST node = null;
         while (itr.hasNext()) {
            if (child != null)
            {
                parent = child;
            }
            node = (LuaAST) itr.next();
            tname = node.getText();
            if (parent != null)
            {
                if (!(parent.type instanceof StructSymbol)) 
                {
                    parent.type = new StructSymbol("table", currentScope);
                }
                scope = (Scope) parent.type;
                child = scope.resolve(tname);
            }
            else
            {
                parent = scope.resolve(tname);
            }
         }
         if (child == null)
         {
             child = new VariableSymbol(tname, type);
             child.def = node;
             node.declaredIn = fileName;
             node.symbol = child; // track in AST
             node.scope = (Scope) parent.type;
             ((StructSymbol) parent.type).define(child);
             //System.out.println("New field TABLE IDENT type" + child);
         }
         return (child);
    }

@Override
  public String getErrorMessage(RecognitionException e, String[] tokenNames) {
    hadErrors = true;
    String msg = super.getErrorMessage(e, tokenNames);
    if ( paraphrases.size()>0 ) {
      String paraphrase = (String)paraphrases.peek();
      msg = msg+" "+paraphrase;
    }
    return msg;
  }		
} 

topdown
    :   enterBlock |  enterFunction | enterFormalParameters
    |   blockStatement
    | returnStarement
    ; 

bottomup
    :   exitBlock | exitFunction | exitFor | exitForIn
;

    
enterBlock :  (BLOCK_SCOPE .*) 
{
}
;
exitBlock : (BLOCK_SCOPE .*)
{ 
};

blockStatement
    :   variableDeclaration 
    |   statement
    ;
        
variableDeclaration 
@init{
  ArrayList rList = new ArrayList();
  ArrayList varList = new ArrayList();
}: 
^(VAR_DECLARATION ^(VAR_DECLARATOR_LIST (^(VAR_DECLARATOR mod=LOCAL? (ident=qualifiedDecl{varList.add(ident);})))+) (^(EXPR(e=expr{rList.add(e.start);})+))*){
    java.util.Iterator lItr = varList.iterator();
    java.util.Iterator rItr = rList.iterator();
    while(lItr.hasNext()){
        Scope tScope = null;
        LuaAST rNode = null;
        if(rItr.hasNext()) rNode = (LuaAST) rItr.next();
        Type type = ((rNode != null) ? rNode.evalType : SymbolTable._void);
        System.out.println("Got ready type " + type  + " for " + $qualifiedDecl.lastNode.getText());
       if(type == null){
            Symbol rsym = currentScope.resolve(rNode.getText());
            if(rsym != null){
                rNode.evalType = rsym.type;
                rNode.type = rsym.type;
                type = rsym.type;
              System.out.println("Got resolved type " + rsym + " for " + $qualifiedDecl.lastNode.getText());
            }
            else {
                type = SymbolTable._void;
            }
        }
          
        qualifiedDecl_return ret = (qualifiedDecl_return) lItr.next();
        java.util.Iterator varItr = ret.variables.iterator();
        Symbol tsym  = null;
        while(varItr.hasNext()){
            LuaAST lNode = (LuaAST) varItr.next();
            if(tScope == null)
            tScope = lNode.scope;
            tsym  = tScope.resolve(lNode.getText());
             //System.out.println("Entering QUAL for " + lNode.getText() + " with Sym " + tsym );
            if(tsym != null && tsym.type instanceof StructSymbol  && varItr.hasNext()){
                tScope = (Scope)tsym.type;
                lNode.declaredIn = tsym.def.declaredIn;
                if(type instanceof StructSymbol){
                    for(Symbol tt : ((StructSymbol) tsym.type).getMembers().values()){
                        ((StructSymbol)type).define(tt);
                    }
                    tsym.type = type;
                    lNode.evalType = type;
                    lNode.type = type;
                    if(lNode.symbol != null)
                      lNode.symbol.type = type;
                }
            }else{
                if(tsym != null){
                    //This is an asignment.
                    if(tsym.type != type){
                        if(tsym.type == SymbolTable._void){
                            tsym.type = type;
                            tsym.def.type = type;
                            System.out.println("Overrode type of " + tsym );
                        }else if (rNode != null) {
                            if(lNode.symbol != null)
                            rNode.setPromoteToType(lNode.symbol.type);
                            type = rNode.getPromoteToType();
                            rNode.evalType = type;
                        }else{
                            System.err.println("Non Compatible assignment of VAR " + tsym + " at line "+lNode.getLine()+ " with " + rNode);
                        }
                    }
                  //System.err.println("Declaring DEFS Dynamic " + tsym);
                }
                if(rNode != null){
                   // lNode.evalType = type;
                   // lNode.type = type;
                   // if(lNode.symbol != null)
                   //     lNode.symbol.type = lNode.type;
                    if(lNode.type == SymbolTable._void){
		                    lNode.evalType = type;
		                    lNode.type = type;
		                    if(lNode.symbol != null)
		                        lNode.symbol.type = lNode.type;
		                }
		                if(rNode.getType() == ARRAY_ELEMENT_ACCESS){
		                    //Propogate type from left to right.
		                    System.out.println("Promotion set for " + rNode + " to " + lNode.evalType);
		                    rNode.promoteToType = lNode.evalType;
		                    $start.promoteToType = lNode.evalType;
		                }
                  if(rNode.evalType == null){
                      if(tsym != null){
                          lNode.evalType = tsym.type;
                          lNode.type = tsym.type;
                          rNode.evalType = tsym.type;
                          rNode.type = tsym.type;
                          if(lNode.symbol != null)
                            lNode.symbol.type = tsym.type;
                      }else{
                        System.err.println("Unable to find symbol <"+symtab.text(rNode)+"> at line "+lNode.getLine());
                      }
                  }
                }
                if(lNode.symbol != null)
                      symtab.declinit(lNode, rNode);
                //System.out.println("VAR DECLARED " + lNode.symbol + " at line "+lNode.getLine()+ " with " + rNode);
           }
        } 
    }
}
| ^(VAR_DECLARATION ^(VAR_DECLARATOR_LIST ^(VAR_DECLARATOR a=arrayElement)) ^(EXPR e=expr)){
      LuaAST lNode = $a.node;
      Type type = lNode.type;
      Type rtype = $e.start.evalType;//getType($e.start);
      Symbol sym = $a.node.scope.resolve($a.node.getText());
      if(sym != null){
          lNode.declaredIn = sym.def.declaredIn;
		      if(sym.type instanceof StructSymbol){
              StructSymbol tSym = (StructSymbol) sym.type;
              Symbol vSym = null;
              if($a.lastNode != null){
		              String name = $a.lastNode.getText();
                      if (name.charAt(0) == '\"')
                          name = name.substring(1,name.length()-1);
		              //System.out.println("Trying to resolve " + name);
		              vSym = tSym.resolveMember(name);
		          }
              if(vSym != null){
                   if(vSym.type == SymbolTable._void){
                       vSym.type = rtype;
                       vSym.def.type = rtype;
                       System.out.println("Overrode type of " + vSym );
                   }else{
                       System.err.println("Non Compatible assignment of VAR " + vSym + " at line "+lNode.getLine()+ " with " + rtype);
                   }
              }else{
                   if($a.lastNode != null)
                  //Not possible!!!
                   System.err.println("Member <" + $a.node.getText() + "> not found!!!");
              }
		      }else{
              System.err.println("Symbol <" + $a.node.getText() + "> is not structure type");
		      }
      }else{
          System.err.println("Unable to find symbol <"+symtab.text(lNode)+"> at line "+lNode.getLine());
      }
  }
;     

enterFunction:^(FUNCTION_METHOD_DECL mod=LOCAL? qualifiedDecl  .*){
    currentScope = $qualifiedDecl.lastNode.scope;
    System.out.println("function line "+$qualifiedDecl.lastNode.getLine()+": def method "+$qualifiedDecl.lastNode.getText() + " in scope " + currentScope);
    $start.declaredIn = fileName; 
    $qualifiedDecl.lastNode.declaredIn = fileName;
    currentMethod = (MethodSymbol) $qualifiedDecl.lastNode.symbol;
};

exitFunction:^(FUNCTION_METHOD_DECL .*){
                  if(globalApi != null && currentMethod != null){
                     MethodSymbol ss = (MethodSymbol) globalApi.resolveMember(currentMethod.def.getText());
                     if(ss != null){
                      Map<String,Symbol> members = currentMethod.getMembers();
                      for(String key : members.keySet()){
                        Symbol par = ss.resolve(key);
                        if(par != null){
                          Symbol ms1 = members.get(key);
                          ms1.type = par.type;
                          ms1.def.type = par.type;
                          ms1.def.evalType = par.type;
                        }
                      }
                     }
                  }
    System.out.println("Exit Function Local ");// + currentScope + " Enclosed in " + currentScope.getEnclosingScope());
    currentScope = currentScope.getEnclosingScope(); 
};
    
enterFormalParameters:formalParameterList;

    
formalParameterList:
            ^(FORMAL_PARAM_LIST formalParameterStandardDecl+ ) 
        |   (FORMAL_PARAM_LIST) //| ^(FORMAL_PARAM_LIST FORMAL_PARAM_VARARG_DECL)
    ;
formalParameterStandardDecl:^(FORMAL_PARAM_STD_DECL IDENT){
//    System.out.println("DEF FORMAL_PARAM_STD_DECL "+ $IDENT.text);
};        

statement
    :   expression | methodCall
    |   ifStatement
    |   enterFor | enterForIn
    |   whileStatement 
    |   repeatStatement
    |   returnStarement
    |   BREAK
//    |   expression
//    |   breakStatement
;

ifStatement:
  ^(IF cond=expression thenBlock=statement elseif=ifStatement)
  {
    symtab.ifstat(cond.tree);
  }
  | ^(IF cond=expression thenBlock=statement  elseBlock=statement)
  {
    symtab.ifstat(cond.tree);
  }
  | ^(IF cond=expression thenBlock=statement)
  {
    symtab.ifstat(cond.tree);
  }
  ////{symtab.ifstat(cond.tree);}
;

whileStatement: ^(WHILE cond=expression statement){
    symtab.ifstat(cond.tree);
};

repeatStatement: ^(REPEAT statement UNTIL cond=expression){
    symtab.ifstat(cond.tree);
};

//returnStarement:   ^(RETURN val=expression?) {
returnStarement:   ^(RETURN val=expression? expression*) {
  $returnStarement.start.symbol = currentMethod;
  System.out.println("Return for function " + currentMethod);
  if(val != null && currentMethod != null){
      Type ty = $val.start.evalType;
      if(ty == null){
          Symbol sym = currentScope.resolve(symtab.text($val.tree));
          if(sym == null){
              System.err.println("Unable to capture return type of " + symtab.text($val.tree));
          }else{
              if(currentMethod.type == SymbolTable._void){
                  $val.start.type = sym.type;
                  $val.start.evalType = sym.type;
                  currentMethod.type = sym.type;
                  currentMethod.def.evalType = sym.type;
                  currentMethod.def.type = sym.type;
              }
          }
      }else{
          if(currentMethod.type == SymbolTable._void){
		          currentMethod.def.evalType = ty;
		          currentMethod.def.type = ty;
              currentMethod.type = ty;
		      }
      }
		  System.out.println("Return Captured " + $val.start.type);
  }
}
    ;
    
enterFor: ^(FOR ^(FOR_INIT IDENT e1=expression) ^(FOR_CONDITION e2=expression) (^(FOR_UPDATE expression))? ^(BLOCK_SCOPE .*))   
{
    symtab.ifstat(e2.tree);
    currentScope = $IDENT.scope; // push scope
   // System.out.println("DEF FIELD "+$IDENT.text);
    $IDENT.evalType = $e1.start.evalType;
    $IDENT.symbol.type = $IDENT.evalType;         // track in AST
};

exitFor:^(FOR .*).*{
   // System.out.println("Popping For Scope " + currentScope);
   // System.out.println("Popped Scope is " + currentScope.getEnclosingScope());
    currentScope = currentScope.getEnclosingScope(); 
};

enterForIn: ^(FOR_EACH ident+=IDENT+ ^(FOR_IN_CONDITION expressionList) ^(BLOCK_SCOPE .*) )   
{
    currentScope = ((LuaAST)$ident.get(0)).scope;
};

exitForIn:^(FOR_EACH .*) .*{
   // System.out.println("Popping ForEach Scope " + currentScope);
   // System.out.println("Popped Scope is " + currentScope.getEnclosingScope());
    currentScope = currentScope.getEnclosingScope(); 
};


/////////     EXPRESSIONS

qualifiedDecl returns [ArrayList variables, LuaAST lastNode, LuaAST firstNode, Symbol symbol]
@init{
  ArrayList list = new ArrayList();
  Scope tScope = currentScope;
}
@after{
  $variables = list;
  $firstNode = (LuaAST)((!list.isEmpty())?list.get(0):$lastNode);
}
:qualifiedMember[list]{
    java.util.Iterator itr = list.iterator();
    Symbol sym = null;
    while(itr.hasNext()){
      LuaAST node = (LuaAST) itr.next();
      sym = tScope.resolve(node.getText());
      //System.out.println("QM Symbol for " + node.getText() + " is " + sym + " in scope " + tScope);
      if(sym == null){
          break;
      }else{
        node.declaredIn = sym.def.declaredIn;
        $start.declaredIn = sym.def.declaredIn;
      }
      if(sym.type instanceof StructSymbol){
         tScope = (Scope) sym.type;
      }
    }
    $lastNode = $qualifiedMember.lastNode;
    $lastNode.symbol = sym;
    $start.evalType = $lastNode.evalType;
}
;

qualifiedName[ArrayList list]  returns [LuaAST node]
@init{
  Scope tScope = currentScope;
}: IDENT{
    if($IDENT.scope != null){
        tScope = $IDENT.scope;
    }
    if(tScope != null){
        Symbol s1 = $IDENT.symbol;
        if(s1 == null)
             s1 = tScope.resolve($IDENT.text);
        if ((s1 != null) && (s1.type != null)) {
          if(s1.type instanceof StructSymbol){
            tScope = (Scope) s1.type;
          }
          $start.evalType = s1.type;
          $IDENT.declaredIn = s1.def.declaredIn;
          $start.declaredIn = s1.def.declaredIn;
        }
   }
   list.add($IDENT);
   $node = $IDENT;
};
qualifiedMember[ArrayList list] returns [LuaAST lastNode]:
      ^(  DOT qualifiedMember[list] qualifiedName[list] )  
        {
            $lastNode = $qualifiedName.node;
            $start.evalType = $lastNode.evalType;
        }
      |
      ^(  COLON qualifiedMember[list] qualifiedName[list] )  
        {
            $lastNode = $qualifiedName.node; 
            $start.evalType = $lastNode.evalType;
        }
      |   qualifiedName[list]
        {
            $lastNode = $qualifiedName.node;
            $start.evalType = $lastNode.evalType;
        }
      | 
      ^(QUALIFIED_TYPE_IDENT (qualifiedName[list])+)
        {
            $lastNode = $qualifiedName.node;
            $start.evalType = $lastNode.evalType;
        }
;

assignment returns [Type type] : ^( ASSIGN a=expr b=expr ) 
{
    //This should happen only in table !!!
	    LuaAST child1 = ((LuaAST)$a.start);
      LuaAST child2 = ((LuaAST)$b.start);
	    if(child1.evalType == StringType && child1.symbol == null){
	        Symbol vSym = getArrayField(child1.getText(), (StructSymbol) currentTable, $start, child1);
	        vSym.type = child2.evalType;
          vSym.def.evalType = child2.evalType;
	        $type = vSym.type;
	        $start.evalType = $type;
         // System.out.println("Got variable " + vSym);
	   }else{
	       if(child1.symbol != null){
           // System.out.println("Got Ready variable " + child1.symbol);
	          $type = child1.symbol.type;
	          $start.evalType = $type;
	       }
	   }
};

expression
    :   ^(EXPR expr) 
    {
        $EXPR.evalType = $expr.type;
    }
;


expr returns [Type type]
@after { 
    $start.evalType = $type; 
}
    :
    assignment {$type = $assignment.type;}
    |    ^(LOGICAL_OR a=expr b=expr){$type = symtab.relop(currentScope, (LuaAST)a.start,(LuaAST)b.start);}
    |   ^(LOGICAL_AND a=expr b=expr){$type = symtab.relop(currentScope, (LuaAST)a.start,(LuaAST)b.start);}

    |   ^(EQUAL a=expr b=expr){$type = symtab.eqop(currentScope, (LuaAST)a.start,(LuaAST)b.start);}
    |   ^(NOT_EQUAL a=expr b=expr){$type = symtab.eqop(currentScope, (LuaAST)a.start,(LuaAST)b.start);}

    |   relationalOp{$type=$relationalOp.type;}
    |   binaryOp {$type=$binaryOp.type;}

    |   ^(CONCAT_STR a=expr b=expr) { 
            $type = StringType;
            Symbol asym = currentScope.resolve(((LuaAST) a.start).getText());
            if ((asym != null) && (asym.type == SymbolTable._void)) {
                 asym.type = $type;
            }
            Symbol bsym = currentScope.resolve(((LuaAST) b.start).getText());
            if ((bsym != null) && (bsym.type == SymbolTable._void)) {
                 bsym.type = $type;
            }
        }
    |   ^(UNARY_MINUS a=expr){$type=symtab.uminus((LuaAST)a.start);}
    |   ^(UNARY_HASH a=expr){$type = SymbolTable._int;}
    |   ^(NOT a=expr){$type=symtab.unot((LuaAST)a.start);}

    |   primaryExpression  {$type = $primaryExpression.type;}
    ;    

binaryOp returns [Type type]:
^((PLUS | MINUS | STAR | DIV | MOD | EXPO ) a=expr b=expr)
{
    $type = symtab.bop(currentScope,$a.start,$b.start);//$a.type;
};

relationalOp returns [Type type]:
^((LESS_OR_EQUAL | GREATER_OR_EQUAL | GREATER_THAN | LESS_THAN ) a=expr b=expr)
{
    $type = symtab.relop(currentScope,$a.start,$b.start);//$a.type;
};    


primaryExpression  returns [Type type]
@after {if($type == null) $type = $start.evalType; else $start.evalType = $type;}
:
       parenthesizedExpression
    |   qualifiedDecl{
        $type = $qualifiedDecl.lastNode.evalType;
        $start.evalType = $type;
    }
    |   methodCall
    |   arrayElement
    |   literal
    |   tableconstructor
    ;

arrayElement returns [Type type, LuaAST node, LuaAST lastNode]:
       ^(ARRAY_ELEMENT_ACCESS a=primaryExpression e=expression) {
//          System.out.println("Array Scope " + currentScope + " enclosed in " + currentScope.getEnclosingScope());
//          System.out.println("Array Param Scope " + $a.start.scope + " enclosed in " + $a.start.scope.getEnclosingScope());
          currentScope =  $a.start.scope;
          Type type = $e.start.evalType;
          Symbol sym = currentScope.resolve($a.start.getText());
          if (sym != null)
          {
              $node = sym.def;
              if (!(sym.type instanceof StructSymbol)) {
                  if (sym.type != SymbolTable._void){
                      System.out.println("Symbol <" + $a.start + "> is not structure type");
                  }
                  StructSymbol symbol = new StructSymbol("table", currentScope);
                  sym.type = symbol;
                  System.out.println("Promoting " + $a.start.getText() + " to " + sym);
              }
          }
          else
          {
              System.err.println("Unable to find symbol <" + $a.start + ">");
          }
          if(type == null){
              if(sym != null){
		              $type = sym.type;
		              $start.evalType = $type;
              }else{
		              $type = SymbolTable._void;
		              $start.evalType = $type;
	            }
          }else{
              if(type != StringType){
//                  System.out.println("RCVD NON STRING TYPE " + type + "  f0r " + sym );
                  //Let us add those too;
                  
		              $type = SymbolTable._void;
		              $start.evalType = $type;
              }else{
		              if ((sym != null) && (sym.type instanceof StructSymbol)) {
		                  StructSymbol tSym = (StructSymbol) sym.type;
		                  String name = $e.start.getChild(0).getText();
		                  Symbol vSym = getArrayField(name, tSym, $start, ((LuaAST)$e.start.getChild(0)));
		                  $type = vSym.type;
		                  $start.evalType = $type;
		                  $lastNode = vSym.def;
		              }else{
		                  $type = SymbolTable._void;
		                  $start.evalType = $type;
		              }
              }
          }
    }
;
methodCall returns [Type type]
@init {List args = new ArrayList();
}:^(METHOD_CALL id=qualifiedDecl  ^(ARGUMENT_LIST ( ^(EXPR (expr {args.add($expr.start);})))*))
    {
		     if(id.lastNode.symbol instanceof MethodSymbol){
		       MethodSymbol sy12 = (MethodSymbol)id.lastNode.symbol;
		       java.util.Iterator itr123 = args.iterator();
		       for(Symbol key : sy12.getMembers().values()){
		         if(itr123.hasNext()){
		           LuaAST n1 = (LuaAST) itr123.next();
		           Type ty = key.type;
		           if(ty != n1.type){
		             System.err.println("Both types are differnt in call of " + id);
		             n1.type = ty;
		             n1.evalType = ty;
		             if(n1.symbol != null){
		               n1.symbol.type = ty;
		               n1.symbol.def.evalType = ty;
		               n1.symbol.def.type = ty;
		             }
		           }
		         }
		       }
		     }
        $type = symtab.call($id.lastNode, args);
        $start.evalType = $type; 
        if($id.lastNode.getText().contains("Form")){
          //System.err.println("DECLARED IN " + $id.lastNode.evalType);
        }
        java.util.Iterator itr = args.iterator();
        while(itr.hasNext()){
          LuaAST node = (LuaAST) itr.next();
          if(node.symbol != null && node.symbol instanceof MethodSymbol){
              node.symbol.def.promoteToType = CallbackPtr;
              //System.err.println("CALLBACK DISCOVERED");
              //node.symbol.def.evalType = CallbackPtr;
              //node.start.promoteToType = 
          }else{
             if(node.getText().equals("startupform")  || node.getText().equals("form1")){
                System.err.println("DECLARED IN XXX");
                
             }
          }
        }
    }
;

arguments
    :   ^(ARGUMENT_LIST expressionList?)
    ;
//tableconstructor  returns [Type type] 
//@init {List args = new ArrayList();
//}: ^(ARRAY_INITIALIZER (^(FIELD_LIST (field)+))?){
//    $type = new StructSymbol("table",null); //BuiltInTypeSymbol("TABLE",SymbolTable.tUSER);
//    
//    $start.evalType = $type; 
//};

tableconstructor  returns [Type type]
@init{
    StructSymbol symbol = new StructSymbol("table",currentScope);
    currentTable = symbol;
}
: ^(ARRAY_INITIALIZER fieldlist?){
    $type = symbol;
    $start.evalType = $type; 
};


fieldlist returns [Type type] 
: ^(FIELD_LIST field+){
    $type = $field.type;
    $start.evalType = $type; 
};

field  returns [Type type] : 
^(ARRAY_ELEMENT_ACCESS  e1=expression e2=expression ){
    $type = symtab.member((LuaAST)$field.start, (LuaAST)e2.start);
    $start.evalType = $type;
} | ^(VAR_DECLARATOR IDENT e1=expression){
    Symbol vs = null;
    Symbol sym = currentScope.resolve($e1.start.getChild(0).getText());
    if (sym instanceof MethodSymbol) {
         ((MethodSymbol) sym).def.promoteToType = CallbackPtr;
         ((MethodSymbol) sym).type = CallbackPtr;
         vs = new MethodSymbol($IDENT.text, $e1.start.evalType, currentTable);
         ((MethodSymbol) vs).getMembers().putAll(((MethodSymbol) sym).getMembers());
         $e1.start.symbol = vs;
    }
    else {
        vs = new VariableSymbol($IDENT.text,$e1.start.evalType);
    }
    vs.def = $IDENT; 
    $IDENT.symbol = vs;         // track in AST
    $IDENT.scope = currentTable;
    currentTable.define(vs);
    $start.evalType = $e1.start.evalType;
    $type = $start.evalType;
//    System.out.println("From Table  TABLE IDENT type" + vs);

} | ^(FIELD  e1=expression){
		    $type = $e1.start.evalType;
		    $start.evalType = $type;
		    $e1.start.scope = currentScope;
//    System.err.println("From Table  TABLE IDENT child " + $e1.start.getChild(0));
    Symbol sym = currentScope.resolve($e1.start.getChild(0).getText());
    if (sym instanceof MethodSymbol) {
         ((MethodSymbol) sym).def.promoteToType = CallbackPtr;
         ((MethodSymbol) sym).type = CallbackPtr;
    }
}
;

parenthesizedExpression 
    :   ^(PARENTESIZED_EXPR expression)
    ;

expressionList: expression+;

literal returns [Type type]
@after { $start.evalType = $type; }
  : 'nil' {$type = SymbolTable._null; $start.getToken().setText("null");}
  | TRUE {$type = SymbolTable._boolean;}
  | FALSE {$type = SymbolTable._boolean;}
  | string  {$type = StringType;}
  | number {$type = SymbolTable._int;}
  | floatLit {$type = SymbolTable._float;}
  | ELLIPSIS {$type = new BuiltInTypeSymbol("...",SymbolTable.tUSER);}
//  | '{}' {$type = new BuiltInTypeSymbol("TABLE",SymbolTable.tUSER);}
  ;


number : INT_LITERAL | HEX_LITERAL;
floatLit: FLOAT_LITERAL | EXP_LITERAL;
string  : NORMALSTRING_LITERAL | CHARSTRING_LITERAL | LONGSTRING_LITERAL;    
