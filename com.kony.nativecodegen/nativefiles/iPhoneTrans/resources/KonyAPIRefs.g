tree grammar KonyAPIRefs;

options {
  language = Java;
  output = AST;
  tokenVocab = KonyAPI;
  ASTLabelType = LuaAST;
  filter = true;
  backtrack = true;
  k = 2;
}


@header {
  package com.konylabs.transformer.api.grammer;
  import com.konylabs.transformer.scopes.*;
}


@members {
    SymbolTable symtab;
    Scope currentScope;
    public boolean hadErrors = false;
    final Type StringType = new BuiltInTypeSymbol("String",SymbolTable.tUSER);
    public KonyAPIRefs(TreeNodeStream input, SymbolTable symtab) {
        this(input);
        this.symtab = symtab;
        currentScope = symtab.globals;
        currentScope.define((Symbol)StringType);
    }
} 

topdown
    :   
        enterBlock | enterStructure | fieldDeclarator 
        ;

    
enterStructure :  ^(STRUCTURE IDENT .*) 
{
     // System.out.println("Entering Struct " + $IDENT.text);
      currentScope = $IDENT.scope;
      Symbol s = currentScope.resolve($IDENT.text);
      currentScope = (Scope)s.type;
}
;
    
enterBlock: ^(BLOCK_SCOPE .*);


fieldDeclarator: ^(VAR_DECLARATOR type IDENT  e=expression[$type.type]?)
{
      Symbol s1 = null;
     // if($IDENT.symbol == null){
          s1 = $IDENT.scope.resolve($IDENT.text);
          //System.err.println("Non Direct Symbol " + s1 );
      //}else{
      //    s1 = $IDENT.symbol;
          //System.err.println("Direct Symbol " + s1 );
      //}
      if(s1 != null){
          s1.type = $type.type;
      }
      if(s1.type == null){
        System.err.println("Null type in declaration for " + s1);
      }
     // System.out.println("Current Var declared " + s1 );
};

expression[Type fType] returns [Type type]
    :   ^(EXPR expr[fType]){
        $EXPR.evalType = $expr.type;
    }
    ;
    
expr[Type fType] returns [Type type]
@after {$type = $start.evalType;}
    :   
    |   literal 
    |   method[fType]
    ;
    
type returns [Type type]:^(TYPE (primitiveType {$type = $primitiveType.type;} | qualifiedTypeIdent {
  $type = $qualifiedTypeIdent.type;
}));


qualifiedTypeIdent returns [Type type]
@init{
  Scope tScope = currentScope;
  ArrayList list = new ArrayList();
}
@after{
  currentScope = tScope;
}

: ^(QUALIFIED_TYPE_IDENT identifier[list]+){
    //System.out.println("QT " + list);
    java.util.Iterator itr = list.iterator();
    int i=0;
    boolean isLast = false;
    Symbol sy = null;
    while(itr.hasNext()){
        i++;
        if(i == list.size()){
            break;
        }
        LuaAST node = (LuaAST) itr.next();
        sy = currentScope.resolve(node.getText());
        if(sy == null){
            System.err.println("Qualified Symbol qual " + node.getText() + " not defined ");
        }else{
            currentScope = (Scope) sy.type;
            $type = sy.type; 
        }
    }
    LuaAST node = (LuaAST) itr.next();
    if(list.size() == 1){
      //Single symbol. Treat it as a type!!!;
      Symbol sy1 = currentScope.resolve(node.getText());
      if(sy1 == null){
          System.err.println("Defining new TYPE :: " + node.getText());
          Symbol sym = new BuiltInTypeSymbol(node.getText(), 0);
          node.scope = currentScope;
          node.symbol = sym;
          symtab.globals.define(sym);
          $type = (Type) sym; 
      }else{
          if(sy1 instanceof BuiltInTypeSymbol)
              $type = (Type) sy1; 
          else $type = sy1.type;
      }
    }else{
      Symbol sy1 = currentScope.resolve(node.getText());
     // System.out.println("Checking QUAL TYPE for {" + node.getText() + "} in " + sy);
      if(sy1 == null){
          System.err.println("Error :: " + node.getText() + " is not a member of " + currentScope);
      }else{
        //System.out.println("Got Type " + sy1.type);
        $type = sy1.type;
      }
//      VariableSymbol vs = new VariableSymbol(node.getText(),null);
//      vs.def = node;
//      vs.scope = currentScope;
//      node.symbol = vs;
//      currentScope.define(vs);
      //$type = (Type) sym; 
    }
};

identifier[List list]: IDENT{
  $list.add($IDENT);
};


//qualifiedTypeIdent returns [Type type, LuaAST lastNode]
//@init{
//  Scope tScope = currentScope;
////  currentScope = symtab.globals;
//}
//@after{
//  currentScope = tScope;
//}
//:qualifiedIdentifier{
//    $lastNode = $qualifiedIdentifier.lastNode;
//    $start.evalType = $lastNode.evalType;
//    $type = $start.evalType;
//}
//identifier returns [LuaAST node] : IDENT {
//  Symbol s1 = $IDENT.symbol;
//  if(s1 == null){
//    s1 = currentScope.resolve($IDENT.text);
//  }
//  if(s1 != null){
//    if(s1 instanceof StructSymbol){
//      currentScope = (Scope) s1.type;
//    }
//  }else{
//    System.err.println("Error : No Symbol defined in defs " + $IDENT.text + " in scope " + currentScope.getScopeName());
//  }
//  $node = $IDENT;
//};
//qualifiedTypeIdent returns [Type type]
//@init{
//  Scope tScope = currentScope;
//}
//@after{
//  currentScope = tScope;
//}
//    :   ^(QUALIFIED_TYPE_IDENT identifier+)
//        {
//            Symbol s = null;
//            if($identifier.node.scope == null){
//                s = currentScope.resolve($identifier.node.getText());
//            }else{
//                s = $identifier.node.scope.resolve($identifier.node.getText());
//            }
//            if ( s!=null ){
//                if(s instanceof BuiltInTypeSymbol){
//                    //System.out.println("String type " + s.type); 
//                    $type = (Type) s;
//                }else{
//                    $type = s.type;
//                } 
//                //$identifier.node.scope = s.scope; 
//            }else{
//                if($identifier.node.getText().equals("handle")){
//                    System.err.println("Handle scope " + $identifier.node.scope.getScopeName());
//                }
//                System.err.println("UNABLE TO RESOLVE QTI " + $identifier.node.getText() + " at line " + $identifier.node.getLine());
//            }
//        }
//    ;

//
//qualifiedName returns [LuaAST node, Type type]
//: IDENT{
//    if(currentScope == null)
//      currentScope = $IDENT.scope;
//    Symbol s1 = $IDENT.symbol;
//    if(s1 == null)
//         s1 = currentScope.resolve($IDENT.text);
//    if(s1 != null){
//      if(s1.type instanceof StructSymbol){
//        currentScope = (Scope) s1.type;
//      }
//      if(s1.type == null){
//      Type ty = null;
//          if(s1 instanceof BuiltInTypeSymbol){
//            ty = (Type) s1;
//          }
//        s1.type = ty;
//      }
//      $start.evalType = s1.type;
//      $type = s1.type;
//    }else{
//        s1 = symtab.globals.resolve($IDENT.text);
//        Type ty = null;
//        if(s1 == null){
//           System.out.println("ERROR : Symbol not found for " + $IDENT.text);
//		        ty = new BuiltInTypeSymbol($IDENT.text, 0);
//		        symtab.globals.define((Symbol)ty);
//           // System.out.println("Declaring type2 " + $IDENT.text + " In scope " + currentScope);
//        }else{
//           System.out.println("WARN : Forcing Symbol " + s1 + " as type for " + $IDENT.text);
//          ty = (Type) s1;
//        }
//        $start.evalType = ty;
//        $type = ty;
//    }
//   $node = $IDENT;
//};
//qualifiedIdentifier returns [LuaAST lastNode, Type type]:
//         qualifiedName
//        {
//            $lastNode = $qualifiedName.node;
//            $start.evalType = $lastNode.evalType;
//            $type = $start.evalType;
//        }
//      | 
//      ^(QUALIFIED_TYPE_IDENT (qualifiedName)+)
//        {
//            $lastNode = $qualifiedName.node;
//            $start.evalType = $lastNode.evalType;
//            $type = $start.evalType;
//        }
//;

    
method[Type fType] returns []: ^(METHOD_CALL IDENT arguments){
    Symbol s = null;
   // System.err.println("Method Start " + $q.lastNode.getText() + " With cuurr " + currentSymbol);
    if($IDENT.scope != null){
      //currentScope = $q.start.scope;
      s = $IDENT.scope.resolve($IDENT.getText());
      if(s != null){
        s.type = fType;
        //System.err.println("Method : " + s + " Type " + fType);
      }
    }
  
};


arguments:^(ARGUMENT_LIST formalParameterList?);
    
formalParameterList: ^(FORMAL_PARAM_LIST formalParameterStandardDecl+ );
    
formalParameterStandardDecl: ^(FORMAL_PARAM_STD_DECL type IDENT){
    $IDENT.symbol.type = $type.type;
};


primitiveType returns [Type type]
@init { 
    $type = (Type)currentScope.resolve($start.getText()); 
    $start.symbol = currentScope.resolve($start.getText()); // return Type
    $start.scope = currentScope;
}

    :   BOOLEAN
    |   CHAR
    |   SHORT
    |   INT
    |   LONG
    |   FLOAT
    |   DOUBLE
    |   VOID
//    |   'string'
//    |   'bool'
    ;


literal returns [Type type]
@after { $start.evalType = $type; }
    :   HEX_LITERAL                 {$type = SymbolTable._int; }
    |   OCTAL_LITERAL               {$type = SymbolTable._int; }
    |   DECIMAL_LITERAL             {$type = SymbolTable._int; }
    |   FLOATING_POINT_LITERAL      {$type = SymbolTable._float; }
    |   CHARACTER_LITERAL           {$type = SymbolTable._char; }
    |   STRING_LITERAL              {$type = StringType;} 
    |   TRUE                        {$type = SymbolTable._boolean; }
    |   FALSE                       {$type = SymbolTable._boolean; }
    |   NULL                        {$type = SymbolTable._null; }
    ;    
    
