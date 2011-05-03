tree grammar LuaDefs;

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
    String fileName = null;
    public LuaDefs(TreeNodeStream input, SymbolTable symtab, String fileName) {
        this(input);
        this.symtab = symtab;
        this.symtab.fileScope = symtab.globals.addFileScope(fileName);
//        this.symtab.fileScope = new LocalScope(fileName, symtab.globals);
        currentScope = this.symtab.fileScope;
        System.out.println("FileName " + fileName);
        this.fileName = fileName;
    }
} 


topdown
    :   enterBlock |  enterFunction | enterFormalParameters
    |   enterFor | enterForIn |  variableDeclaration | methodCall | tableconstructor | arrayAccess
    |   returnStarement
    ; 

bottomup
    :   exitBlock | exitFunction | exitFor | exitForIn
;

    
enterBlock :  (BLOCK_SCOPE .*) 
{
    currentScope = new LocalScope(currentScope); // push scope
}
;
exitBlock : (BLOCK_SCOPE .*)
{ 
    currentScope = currentScope.getEnclosingScope(); 
};

//arrayDeclaration 
//@init{
//  ArrayList varList = new ArrayList();
//}: 
//^(VAR_DECLARATION ^(VAR_DECLARATOR_LIST (^(VAR_DECLARATOR ^(ARRAY_ELEMENT_ACCESS (ident=IDENT{varList.add(ident);}) .*)))+) .*){
//};
variableDeclaration 
@init{
  ArrayList varList = new ArrayList();
}: 
^(VAR_DECLARATION ^(VAR_DECLARATOR_LIST (^(VAR_DECLARATOR mod=LOCAL? (ident=qualifiedDecl{varList.add(ident);})))+) .*){
    java.util.Iterator lItr = varList.iterator();
    boolean isScoped = false;
    Scope tScope = currentScope;
    isScoped = (mod != null);

    while(lItr.hasNext()){
        boolean bRet = false;
        qualifiedDecl_return ret = (qualifiedDecl_return) lItr.next();
        java.util.Iterator varItr = ret.variables.iterator();
        
        while(varItr.hasNext()){
            LuaAST lNode = (LuaAST) varItr.next();
            Symbol tsym  = null;
            if (tScope instanceof StructSymbol) {
                 tsym = ((StructSymbol) tScope).resolveMember(lNode.getText());
            }
            else {
                tsym = tScope.resolve(lNode.getText());
            }
            
            if(lNode.getText().equals(fileName)){
                lNode.getToken().setText(lNode.getText().toLowerCase());
            }
		        
            if(varItr.hasNext()){
                if(tsym != null){
                    if(tsym.type instanceof StructSymbol){
                        //Do nothing...
                        tScope = (Scope) tsym.type;
                        isScoped = true;
                        
                    }else{
		                    StructSymbol symbol = new StructSymbol("table",tScope);
		                    tsym.type = (Type) symbol;
		                    tScope = (Scope) symbol;
		                    isScoped = true;
                        System.out.println("Promoting  " + lNode.getText() + " to a table " + lNode.type + " :: " + lNode.evalType );
		                }
                }
            }else {
                 if ((tsym != null) && (tsym.type instanceof StructSymbol) &&
                     (tsym.def != null)) {
                     if (!(tsym.def.declaredIn.equals(fileName))) {
                         tsym.def.declaredIn = fileName;
                         lNode.isDeclaration = true;
                     }
                 }
            }
		        if(tsym == null){ 
		            Type newtype = SymbolTable._void;
		            if(varItr.hasNext()){
		                newtype = new StructSymbol("table",tScope);
		            }
                VariableSymbol vs = new VariableSymbol(lNode.getText(),SymbolTable._void);
                vs.def = lNode;            // track AST location of def's ID
                lNode.scope = tScope;
                lNode.declaredIn = fileName;
                lNode.isDeclaration = true;
                lNode.symbol = vs;         // track in AST
		            if(isScoped){
		              lNode.scope = tScope;
		              tScope.define(vs);
		              if(!(tScope instanceof StructSymbol)){
		                  $start.isDeclaration = true;
		              }
		            }else{
			            lNode.scope = tScope;
			            symtab.globals.define(vs);
                  $start.isDeclaration = true;
		            }
		            if($start.isDeclaration){
		              System.out.println("Declaring DEFS " + lNode.symbol + " in scope " + tScope.getEnclosingScope());
		            }
		            if (newtype instanceof StructSymbol) {
                    tScope = (Scope) newtype;
                    isScoped = true;
                }
		            
		        }
        } 
    }
};     

enterFunction:^(FUNCTION_METHOD_DECL mod=LOCAL? qualifiedDecl  .*){
    java.util.Iterator itr = $qualifiedDecl.variables.iterator();
    Scope tScope = currentScope;
    while(itr.hasNext()){
        LuaAST tNode = (LuaAST) itr.next();
        Symbol tSym = tScope.resolve(tNode.getText());
        if(tSym != null && tSym.type instanceof StructSymbol){
            tScope = (Scope)tSym.type;
        }
        if(tSym == null && tNode == $qualifiedDecl.lastNode){
				    MethodSymbol method = new MethodSymbol($qualifiedDecl.lastNode.getText(),SymbolTable._void,tScope);
				    method.def =$qualifiedDecl.lastNode;
            method.def.declaredIn = fileName;
            method.def.isDeclaration = true;
				    $qualifiedDecl.lastNode.symbol = method;        // track in AST
				    if($mod != null){
				        $qualifiedDecl.lastNode.scope = tScope;
				        tScope.define(method);
//                $start.isLocal = true;
//                method.def.isLocal = true;
				    }else{
				        $qualifiedDecl.lastNode.scope = tScope;
				          symtab.globals.define(method);
//                tNode.isLocal = false;
//                $start.isLocal = false;
//                method.def.isLocal = false;
				    }
				    currentScope = method;       // set current scope to method scope
				    currentMethod = method;
				    break;
        }
    }
};

exitFunction:^(FUNCTION_METHOD_DECL .*){
    currentScope = currentScope.getEnclosingScope(); 
};
    
enterFormalParameters:formalParameterList;

    
formalParameterList:
            ^(FORMAL_PARAM_LIST formalParameterStandardDecl+ ) 
        |   (FORMAL_PARAM_LIST) //| ^(FORMAL_PARAM_LIST FORMAL_PARAM_VARARG_DECL)
    ;
formalParameterStandardDecl:^(FORMAL_PARAM_STD_DECL IDENT){
    VariableSymbol vs = new VariableSymbol($IDENT.text,SymbolTable._void);
    vs.def = $IDENT;
    $IDENT.symbol = vs;         // track in AST
    $IDENT.scope = currentScope;
    currentScope.define(vs);
    $IDENT.isDeclaration = true;
};        

returnStarement:   ^(RETURN .*) {
  $returnStarement.start.symbol = currentMethod;
};

enterFor: ^(FOR ^(FOR_INIT IDENT e1=.*) ^(FOR_CONDITION e2=.*) (^(FOR_UPDATE .*))? ^(BLOCK_SCOPE .*))   
{
    currentScope = new LocalScope("forScope" , currentScope); // push scope
    VariableSymbol vs = new VariableSymbol($IDENT.text,SymbolTable._void);
    vs.def = $IDENT;
    $IDENT.symbol = vs;         // track in AST
    $IDENT.scope = currentScope;
    $IDENT.isDeclaration = true;
    currentScope.define(vs); 
};

exitFor:^(FOR .*).*{
    currentScope = currentScope.getEnclosingScope(); 
};

enterForIn: ^(FOR_EACH ident+=IDENT+ ^(FOR_IN_CONDITION .) ^(BLOCK_SCOPE .*) )   
{
    currentScope = new LocalScope("forScope" , currentScope); // push scope
    java.util.Iterator itr = $ident.iterator();
    LuaAST node = null;
    while(itr.hasNext()){
        node = (LuaAST) itr.next();
        VariableSymbol vs = new VariableSymbol(node.getText(),SymbolTable._void);
        vs.def = node;
        node.symbol = vs;         // track in AST
        node.scope = currentScope;
        node.isDeclaration = true;
        currentScope.define(vs);
    } 
};

exitForIn:^(FOR_EACH .*) .*{
    currentScope = currentScope.getEnclosingScope(); 
};


methodCall:^(METHOD_CALL id=qualifiedDecl  ^(ARGUMENT_LIST .*))
    {
        $id.lastNode.scope = currentScope;
        $start.scope = currentScope;
    }
;

qualifiedDecl returns [ArrayList variables, LuaAST lastNode, LuaAST firstNode]
@init{
  ArrayList list = new ArrayList();
}
@after{
  $variables = list;
  $firstNode = (LuaAST)((!list.isEmpty())?list.get(0):$lastNode);
 // System.out.println("QUAL :: " + list);
}
:qualifiedMember[list]{
    $lastNode = $qualifiedMember.lastNode;
}
;

qualifiedName[ArrayList list]  returns [LuaAST node]
: IDENT{
    $IDENT.scope = currentScope;
	   list.add($IDENT);
	   $node = $IDENT;
};
qualifiedMember[ArrayList list] returns [LuaAST lastNode]:
      ^(  DOT qualifiedMember[list] qualifiedName[list] )  
        {
            $lastNode = $qualifiedName.node;
        }
      |
      ^(  COLON qualifiedMember[list] qualifiedName[list] )  
        {
            $lastNode = $qualifiedName.node; 
        }
      |   qualifiedName[list]
        {
            $lastNode = $qualifiedName.node;
        }
      | 
      ^(QUALIFIED_TYPE_IDENT (qualifiedName[list])+)
        {
            $lastNode = $qualifiedName.node;
        }
;

arrayAccess: ^(ARRAY_ELEMENT_ACCESS e1=. e2=.){
          $e1.scope = currentScope;
          $start.scope = currentScope;
    };


tableconstructor : ^(ARRAY_INITIALIZER fieldlist?){
  //$start.declaredIn = fileName;
  $start.scope = currentScope;
};

fieldlist : ^(FIELD_LIST field+);

field  returns [Type type] : 
^(ARRAY_ELEMENT_ACCESS f=field .* ){
  $f.start.scope = currentScope;
}
| ^(VAR_DECLARATOR IDENT .*){
    $IDENT.scope = currentScope;
}
| ^(FIELD  .*);
 
