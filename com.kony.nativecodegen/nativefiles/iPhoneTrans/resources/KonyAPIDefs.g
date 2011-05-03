tree grammar KonyAPIDefs;

options {
  output = AST;
  tokenVocab = KonyAPI;
  ASTLabelType = LuaAST;
  filter = true;
//  backtrack = true;
}


@header {
  package com.konylabs.transformer.api.grammer;
  import com.konylabs.transformer.scopes.*;
}


@members {
    SymbolTable symtab;
    Scope currentScope;
    StructSymbol currentTable;
    public boolean hadErrors = false;
    public KonyAPIDefs(TreeNodeStream input, SymbolTable symtab) {
        this(input);
        this.symtab = symtab;
        currentScope = symtab.globals;
    }
} 

topdown
    :   
        enterBlock | enterStructure | fieldDeclarator | enterFunction | enterArguments 
//    |   fieldDeclarator
    |   idref    ;

bottomup
    :    
        exitStructure | exitFunction
    ;

enterStructure :  ^(STRUCTURE IDENT .*) 
{
    StructSymbol symbol = new StructSymbol($IDENT.text, currentScope);
    VariableSymbol vs = new VariableSymbol($IDENT.text, (Type) symbol);
    $IDENT.scope = currentScope;
    $IDENT.symbol = vs;
    vs.def = $IDENT;
    currentScope.define(vs);
    //currentScope = new LocalScope(currentScope);
    currentScope = (Scope) symbol; // push scope
//    System.out.println("DEF STRUCT " + $IDENT.text);
}
;
exitStructure : ^(STRUCTURE IDENT .*)
{ 
//    System.out.println("Exit Structure " + currentScope.getScopeName() + " :: " +  currentScope); 
    currentScope = currentScope.getEnclosingScope(); 
};
    
enterBlock: ^(BLOCK_SCOPE .*);


fieldDeclarator: ^(VAR_DECLARATOR type IDENT  .*){
    VariableSymbol vs = new VariableSymbol($IDENT.text, null);
    currentScope.define(vs);
    $IDENT.scope = currentScope;
    $IDENT.symbol = vs;
    vs.def = $IDENT;
//    System.out.println("DEF VAR " + vs);
};

type:^(TYPE (primitiveType  | qualifiedTypeIdent));

qualifiedTypeIdent 
: ^(QUALIFIED_TYPE_IDENT identifier+);

identifier: IDENT{
  $IDENT.scope = currentScope;
};

//qualifiedIdentifier returns [LuaAST lastNode]
//    :   (   id1=IDENT {$lastNode= $id1;
//            $id1.scope = currentScope;
//        }               
//        )
//        (   ^(DOT qualifiedIdentifier id2=IDENT) {
//            $lastNode= $id2;
//            $id2.scope = currentScope;
//        }
//        )*
//    ;
    
enterFunction: ^(METHOD_CALL IDENT .*){
    //System.out.println("Entering method " + $IDENT.text);
    MethodSymbol method = new MethodSymbol($IDENT.getText(),null,currentScope);
    method.def =$IDENT;
    $IDENT.symbol = method;        // track in AST
    $IDENT.scope = currentScope;
    currentScope.define(method);
    currentScope = method;
};

exitFunction : ^(METHOD_CALL .*)
{ 
//    System.out.println("Method " + currentScope); 
    currentScope = currentScope.getEnclosingScope(); 
};

enterArguments:^(ARGUMENT_LIST formalParameterList?);
    
formalParameterList: ^(FORMAL_PARAM_LIST formalParameterStandardDecl+ );
    
formalParameterStandardDecl: ^(FORMAL_PARAM_STD_DECL type? IDENT){
    VariableSymbol vs = new VariableSymbol($IDENT.text, null);
    currentScope.define(vs);
    $IDENT.scope = currentScope;
    $IDENT.symbol = vs;
    vs.def = $IDENT;
};

primitiveType 
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


literal 
    :   HEX_LITERAL
    |   OCTAL_LITERAL
    |   DECIMAL_LITERAL
    |   FLOATING_POINT_LITERAL
    |   CHARACTER_LITERAL
    |   STRING_LITERAL
    |   TRUE
    |   FALSE
    |   NULL
    ;

idref
    :  {$start.hasAncestor(EXPR)||$start.hasAncestor(ASSIGN)}? IDENT
        {
            $start.scope = currentScope;
        }
    ;