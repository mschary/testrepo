/*
 * Lua 5.1 grammar
 * 
 * Aasim Kumar Sokal
 * Konylabs
 *
 */


grammar Lua;

options {
    backtrack = true; 
    memoize = true;
    output = AST;
    ASTLabelType = LuaAST;
}

tokens {
    // operators and other special chars
    
    ASSIGN                  = '='               ;
    COLON                   = ':'               ;
    COMMA                   = ','               ;
    DIV                     = '/'               ;
    DOT                     = '.'               ;
    ELLIPSIS                = '...'             ;
    EQUAL                   = '=='              ;
    GREATER_OR_EQUAL        = '>='              ;
    GREATER_THAN            = '>'               ;
    LBRACK                  = '['               ;
    LCURLY                  = '{'               ;
    LESS_OR_EQUAL           = '<='              ;
    LESS_THAN               = '<'               ;
    LOGICAL_AND             = 'and'              ;
    NOT                     = 'not'               ;
    LOGICAL_OR              = 'or'              ;
    LPAREN                  = '('               ;
    MINUS                   = '-'               ;
    MOD                     = '%'               ;
    NOT_EQUAL               = '~='              ;
    PLUS                    = '+'               ;
    RBRACK                  = ']'               ;
    RCURLY                  = '}'               ;
    RPAREN                  = ')'               ;
    SEMI                    = ';'               ;
    STAR                    = '*'               ;
    EXPO                     = '^'               ;
    HASH                    = '#'              ;
    CONCAT_STR              = '..';
    FOR_IN_CONDITION;
    UNARY_HASH;

    // Java keywords
    
    ABSTRACT                = 'abstract'        ;
    ASSERT                  = 'assert'          ;
    BOOLEAN                 = 'boolean'         ;
    BREAK                   = 'break'           ;
    BYTE                    = 'byte'            ;
    CASE                    = 'case'            ;
    CATCH                   = 'catch'           ;
    CHAR                    = 'char'            ;
    CLASS                   = 'class'           ;
    CONTINUE                = 'continue'        ;
    DEFAULT                 = 'default'         ;
    DO                      = 'do'              ;
    DOUBLE                  = 'double'          ;
    ELSE                    = 'else'            ;
    ENUM                    = 'enum'            ;
    EXTENDS                 = 'extends'         ;
    FALSE                   = 'false'           ;
    FINAL                   = 'final'           ;
    FINALLY                 = 'finally'         ;
    FLOAT                   = 'float'           ;
    FOR                     = 'for'             ;
    IF                      = 'if'              ;
    IMPLEMENTS              = 'implements'      ;
    INSTANCEOF              = 'instanceof'      ;
    INTERFACE               = 'interface'       ;
    IMPORT                  = 'import'          ;
    INT                     = 'int'             ;
    LONG                    = 'long'            ;
 //   NATIVE                  = 'native'          ;
    NEW                     = 'new'             ;
    NULL                    = 'null'            ;
    PACKAGE                 = 'package'         ;
    PRIVATE                 = 'private'         ;
    PROTECTED               = 'protected'       ;
    PUBLIC                  = 'public'          ;
    RETURN                  = 'return'          ;
    SHORT                   = 'short'           ;
    STATIC                  = 'static'          ;
    STRICTFP                = 'strictfp'        ;
    SUPER                   = 'super'           ;
    SWITCH                  = 'switch'          ;
    SYNCHRONIZED            = 'synchronized'    ;
    THIS                    = 'this'            ;
    THROW                   = 'throw'           ;
    THROWS                  = 'throws'          ;
    TRANSIENT               = 'transient'       ;
    TRUE                    = 'true'            ;
    TRY                     = 'try'             ;
    VOID                    = 'void'            ;
    VOLATILE                = 'volatile'        ;
    WHILE                   = 'while'           ;

    // Lua keywords
    DO                      = 'do'              ;
    END                     = 'end'          ;
    FUNCTION                = 'function';
    LOCAL                   = 'local';
    THEN                    = 'then';
    REPEAT                  = 'repeat';
    UNTIL                   = 'until';
    ELSEIF                  = 'elseif';
    IN                      = 'in';



    // tokens for imaginary nodes
    
    ARGUMENT_LIST;
    ARRAY_DECLARATOR;
    ARRAY_DECLARATOR_LIST;
    ARRAY_ELEMENT_ACCESS;
    ARRAY_INITIALIZER;
    BLOCK_SCOPE;
    EXPR;
    FOR_CONDITION;
    FOR_EACH;
    FOR_INIT;
    FOR_UPDATE;
    FORMAL_PARAM_LIST;
    FORMAL_PARAM_STD_DECL;
    FORMAL_PARAM_VARARG_DECL;
    FUNCTION_METHOD_DECL;
    LUA_SOURCE;
    METHOD_CALL;
    PARENTESIZED_EXPR;
    QUALIFIED_TYPE_IDENT;
    TYPE;
    UNARY_MINUS;
    UNARY_PLUS;
    VAR_DECLARATION;
    VAR_DECLARATOR;
    VAR_DECLARATOR_LIST;
    VOID_METHOD_DECL;
    FIELD;
    FIELD_LIST;
}

@header {
  package com.konylabs.transformer.lua.grammer;
  import com.konylabs.transformer.scopes.*;
}

@lexer::header {
  package com.konylabs.transformer.lua.grammer;
}

@lexer::members {
  public boolean preserveWhitespacesAndComments = true;
}

// Starting point for parsing a Java file.
compilationUnit
    :   blockStatement
        ->  ^(LUA_SOURCE blockStatement)
    ;

blockStatement : (statement (SEMI!)?)* (laststat (SEMI!)?)?;

block : blockStatement ->  ^(BLOCK_SCOPE blockStatement?);


statement :  
  listInit | 
  functioncall | 
  statementBlock| 
  WHILE exp DO block END   -> ^(WHILE exp block)| 
  REPEAT block UNTIL exp   ->  ^(REPEAT block UNTIL exp)| 
  ifStatement | 
  'for' forInit COMMA forCondition forUpdater? statementBlock ->    ^(FOR forInit forCondition forUpdater? statementBlock)| 
  'for' IDENT (',' IDENT)* IN expressionList statementBlock -> ^(FOR_EACH IDENT+ ^(FOR_IN_CONDITION expressionList)  statementBlock)| 
  methodDeclaration | 
  localVariableDeclaration
  ;
  
statementBlock:
  DO! block END!
; 
//statement
//  : statementBlock
//  | variableStatement
//  | emptyStatement
//  | expressionStatement
//  | ifStatement
//  | iterationStatement
//  | continueStatement
//  | breakStatement
//  | returnStatement
//  | withStatement
//  | labelledStatement
//  | switchStatement
//  | throwStatement
//  | tryStatement
//  ;
ifStatement:
  IF! ifPart END!
;

ifPart:
  exp THEN ifStat=block    
  ( ELSEIF sifPart=ifPart    -> ^(IF exp $ifStat $sifPart)
    | ELSE eStat=block  -> ^(IF exp $ifStat  $eStat?)
    |           -> ^(IF exp $ifStat)
  )
;

forInit:  IDENT ASSIGN exp 
      ->  ^(FOR_INIT IDENT exp)
;

//forVariableDeclaration
//    :   IDENT ASSIGN exp
//        ->  ^(VAR_DECLARATOR IDENT exp)
//    ;
forCondition:
   exp ->  ^(FOR_CONDITION exp)
;
    
forUpdater:
   COMMA exp
        ->  ^(FOR_UPDATE exp)
;


//qualifiedIdentifierfunc
//    :   (   IDENT               ->  IDENT
//        )
//        (   DOT ident=IDENT     ->  ^(DOT $qualifiedIdentifierfunc $ident)
//        )*
//        (   COLON ident=IDENT     ->  ^(COLON $qualifiedIdentifierfunc $ident)
//        )?
//    ;
qualifiedIdentifierfunc
    :   (
          IDENT (DOT IDENT)*   ->  ^(QUALIFIED_TYPE_IDENT IDENT+)
        ) 
        (   COLON ident=IDENT     ->  ^(COLON $qualifiedIdentifierfunc $ident)
        )?
;
    
//qualifiedDotIdentifier : IDENT (DOT IDENT)* (COLON IDENT)? -> ^(DOT IDENT+ ^(COLON IDENT)?);    
methodDeclaration:
  'function' qualifiedIdentifierfunc funcbody -> ^(FUNCTION_METHOD_DECL qualifiedIdentifierfunc funcbody)| 
  'local' 'function' IDENT funcbody -> ^(FUNCTION_METHOD_DECL LOCAL IDENT funcbody)
; 

localVariableDeclaration:
'local' namelist ('=' expressionList)? ->  ^(VAR_DECLARATION namelist (expressionList)?)
;
laststat : RETURN (expressionList)? -> ^(RETURN expressionList?)| 'break' ->^(BREAK);
//laststat : RETURN (exp)? -> ^(RETURN exp?)| 'break' ->^(BREAK);

varlist1 : varDecl (',' varDecl)* ->  ^(VAR_DECLARATOR_LIST varDecl+);

varDecl:
  qualifiedIdentExpression  -> ^(VAR_DECLARATOR qualifiedIdentExpression)
;

listInit:
varlist1 ASSIGN expressionList -> ^(VAR_DECLARATION varlist1 expressionList)
  ;

namelist : name (',' name)* -> ^(VAR_DECLARATOR_LIST name+);

name:IDENT -> ^(VAR_DECLARATOR LOCAL IDENT);

//explist1 : (exp ',')* exp;
expressionList
    :   exp (COMMA! exp)*
    ;

//exp :  ('nil' | 'false' | 'true' | number | string | '...' | function | prefixexp | tableconstructor | unop exp) (binop exp)* ;  

exp : assignmentExpression  ->  ^(EXPR assignmentExpression);

assignmentExpression
    :   inclusiveOrExpression /*| leftHandSideExpression*/
        (   (   ASSIGN^
        ) 
        assignmentExpression)?
    ;
    
inclusiveOrExpression
    :   andExpression (LOGICAL_OR^ andExpression)*
    ;

andExpression
    :   relationalExpression (LOGICAL_AND^ relationalExpression)*
    ;

relationalExpression
    :   equalityExpression 
        (   (   LESS_OR_EQUAL^
            |   GREATER_OR_EQUAL^
            |   LESS_THAN^
            |   GREATER_THAN^
            )
            equalityExpression
        )*
    ;

equalityExpression
    :   concatExpression 
        (   (   EQUAL^
            |   NOT_EQUAL^
            ) 
            concatExpression
        )*
    ;
concatExpression
    :   additiveExpression 
        (   (   CONCAT_STR^
        ) 
        additiveExpression)*
    ;    
    
additiveExpression
    :   multiplicativeExpression
        (   (   PLUS^
            |   MINUS^
            )
            multiplicativeExpression
        )*
    ;

multiplicativeExpression
    :   powExpression 
        (   (   STAR^
            |   DIV^
            |   MOD^
            )
            powExpression
        )*
    ;
powExpression
    :   unaryExpression 
        (   EXPO^
            unaryExpression
        )*
    ;    
unaryExpression
    :   
        MINUS unaryExpression       ->  ^(UNARY_MINUS[$MINUS, "UNARY_MINUS"] unaryExpression)
    |   unaryExpressionNotPlusMinus
    ;

unaryExpressionNotPlusMinus
    :   NOT unaryExpression                             ->  ^(NOT unaryExpression)
    |   HASH unaryExpression                            ->  ^(UNARY_HASH unaryExpression)
    |   primaryExpression
    ;  

leftHandSideExpression:
  'nil' | 'false' | 'true' | number | string | '...' | /*function |*/ prefixexp | tableconstructor;


primaryExpression
    :   tableconstructor | /*functioncall |*/ prefixexp //| function
    |   literal
    |   qualifiedIdentExpression
    ;
      
indexSuffix
  :  LBRACK exp RBRACK                ->  ^(ARRAY_ELEMENT_ACCESS [$LBRACK, "ARRAY_ELEMENT_ACCESS"] exp)
  ; 
  
propertyName
  : IDENT
  | string
  ;  
//var: (NAME | '(' exp ')' varSuffix) varSuffix*;

qualifiedIdentifier
    :   (   IDENT               ->  IDENT
        )
        (   COLON ident=IDENT     ->  ^(COLON $qualifiedIdentifier $ident)
        )?
    ;


qualifiedIdentExpression: 
  (    
      (
          qualifiedIdentifier             ->     qualifiedIdentifier
          
      ) 
                                
      (
          (
            //(COLON ident=IDENT    ->  ^(COLON $qualifiedIdentExpression $ident))?
            args            ->  ^(METHOD_CALL $qualifiedIdentExpression args)
          )*  
                               
          (
              LBRACK exp RBRACK          ->  ^(ARRAY_ELEMENT_ACCESS $qualifiedIdentExpression exp)
              | 
              DOT ident=IDENT             ->     ^(DOT $qualifiedIdentExpression $ident)  
          )
      )*   
  | 
      (parenthesizedExpression -> parenthesizedExpression)
      (
          (
            (COLON ident=IDENT          ->  ^(COLON $qualifiedIdentExpression $ident))?      
            args                        ->  ^(METHOD_CALL $qualifiedIdentExpression args)
          )* 
          (
              LBRACK exp RBRACK          ->  ^(ARRAY_ELEMENT_ACCESS $qualifiedIdentExpression exp)
              | 
              DOT ident=IDENT                  ->  ^(DOT $qualifiedIdentExpression $ident) 
          )
      )*
      
  ) ;
  
//qualifiedIdentExpression: 
//  IDENT 
//  (((':' IDENT)? args)* (indexSuffix | propertyReferenceSuffix))*
//| parenthesizedExpression (((':' IDENT)? args)* (indexSuffix | DOT IDENT))+;

/*
prefixexp: 
    qualifiedIdentExpression ((COLON  IDENT)? args)* 
| parenthesizedExpression((COLON ident=IDENT)?      
        args)*      
;
*/

prefixexp: 
   ( qualifiedIdentExpression -> qualifiedIdentExpression)
    (
        (COLON ident=IDENT          ->  ^(COLON $prefixexp $ident))?      
         args  ->  ^(METHOD_CALL $prefixexp args)
    )* 
    | 
    parenthesizedExpression 
   ( parenthesizedExpression -> parenthesizedExpression)
    (
        (COLON ident=IDENT          ->  ^(COLON $prefixexp $ident))?      
         args  ->  ^(METHOD_CALL $prefixexp args)
    )* 
;

/*
functioncall: 
    qualifiedIdentExpression ((COLON  IDENT)?      
        args)+ ->  ^(METHOD_CALL qualifiedIdentExpression ((COLON IDENT)? args)+)
    | 
    parenthesizedExpression ((COLON IDENT)?      
        args)+  ->  ^(METHOD_CALL parenthesizedExpression ((COLON IDENT)?      
        args)+)
;
*/
functioncall: 
   ( qualifiedIdentExpression -> qualifiedIdentExpression)
    (
        (COLON ident=IDENT          ->  ^(COLON $functioncall $ident))?      
         args  ->  ^(METHOD_CALL $functioncall args)
    )+ 
    | 
    parenthesizedExpression 
   ( parenthesizedExpression -> parenthesizedExpression)
    (
        (COLON ident=IDENT          ->  ^(COLON $functioncall $ident))?      
         args  ->  ^(METHOD_CALL $functioncall args)
    )+ 
;

/*
var :  NAME | prefixexp '[' exp ']' | prefixexp '.' NAME; 

prefixexp : var | functioncall | '(' exp ')';

functioncall :  prefixexp args | prefixexp ':' NAME args ;
*/

//varOrExp: qualifiedIdentExpression | parenthesizedExpression;
 

parenthesizedExpression
    :   LPAREN exp RPAREN
        ->  ^(PARENTESIZED_EXPR[$LPAREN, "PARENTESIZED_EXPR"] exp)
    ;

args
    :   LPAREN expressionList? RPAREN
        ->  ^(ARGUMENT_LIST[$LPAREN, "ARGUMENT_LIST"] expressionList?) 
        | tableconstructor | string 
    ;

formalParameterList
    :   LPAREN 
        (   // Contains at least one standard argument declaration and optionally a variable argument declaration.
            formalParameterStandardDecl (COMMA formalParameterStandardDecl)* (COMMA formalParameterVarArgDecl)? 
            ->  ^(FORMAL_PARAM_LIST[$LPAREN, "FORMAL_PARAM_LIST"] formalParameterStandardDecl+ formalParameterVarArgDecl?) 
            // Contains a variable argument declaration only.
        |   formalParameterVarArgDecl
            ->  ^(FORMAL_PARAM_LIST[$LPAREN, "FORMAL_PARAM_LIST"] formalParameterVarArgDecl) 
            // Contains nothing.
        |   ->  ^(FORMAL_PARAM_LIST[$LPAREN, "FORMAL_PARAM_LIST"]) 
        )
        RPAREN
    ;
    
formalParameterStandardDecl
    :   IDENT
        ->  ^(FORMAL_PARAM_STD_DECL IDENT)
    ;
    
formalParameterVarArgDecl
    :   ELLIPSIS
        ->  ^(FORMAL_PARAM_VARARG_DECL)
    ;

function : FUNCTION funcbody; //No support for inner functions

funcbody : formalParameterList block END!;

tableconstructor : LCURLY (fieldlist)? RCURLY ->  ^(ARRAY_INITIALIZER[$LCURLY, "ARRAY_INITIALIZER"] fieldlist?)
 ;

fieldlist : field (fieldsep field)* (fieldsep)?     -> ^(FIELD_LIST field+);

field : 
LBRACK exp1=exp RBRACK ASSIGN exp2=exp         ->  ^(ARRAY_ELEMENT_ACCESS $exp1 $exp2 )
      | IDENT ASSIGN exp                 ->  ^(VAR_DECLARATOR IDENT exp)
      | exp    -> ^(FIELD  exp);
fieldsep : ',' | ';';

number : INT_LITERAL | FLOAT_LITERAL | EXP_LITERAL | HEX_LITERAL;

string  : NORMALSTRING_LITERAL | CHARSTRING_LITERAL | LONGSTRING_LITERAL;


literal
  : 'nil'
  | 'true'
  | 'false'
  | string
  | number 
  | ELLIPSIS
  ;

// LEXER

IDENT :('a'..'z'|'A'..'Z'|'_')(options{greedy=true;}: 'a'..'z'|'A'..'Z'|'_'|'0'..'9')*
  ;

INT_LITERAL : ('0'..'9')+;

FLOAT_LITERAL   :INT_LITERAL '.' INT_LITERAL ;

EXP_LITERAL : (INT| FLOAT) ('E'|'e') ('-')? INT_LITERAL;

HEX_LITERAL :'0x' ('0'..'9'|'a'..'f'|'A'..'F')+ ;

  

NORMALSTRING_LITERAL
    :  '"' ( EscapeSequence | ~('\\'|'"') )* '"' 
    ;

CHARSTRING_LITERAL
   :  '\'' ( EscapeSequence | ~('\''|'\\') )* '\''
   ;

LONGSTRING_LITERAL
  : '['('=')*'[' ( EscapeSequence | ~('\\'|']') )* ']'('=')*']'
  ;

fragment
EscapeSequence
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UnicodeEscape
    |   OctalEscape
    ;
    
fragment
OctalEscape
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;
    
fragment
UnicodeEscape
    :   '\\' 'u' HexDigit HexDigit HexDigit HexDigit
    ;
    
fragment
HexDigit : ('0'..'9'|'a'..'f'|'A'..'F') ;


COMMENT
    :   '--[[' ( options {greedy=false;} : . )* ']]' 
    {
        if (!preserveWhitespacesAndComments) {
            skip();
        } else {
            $channel = HIDDEN;
        }
    }
    ;
    
LINE_COMMENT
    : '--' ~('\n'|'\r')* '\r'? '\n' 
    {
        if (!preserveWhitespacesAndComments) {
            skip();
        } else {
            $channel = HIDDEN;
        }
    }
    ;
    
    
WS  :  (' '|'\t'|'\u000C') {skip();}
    ;
    
NEWLINE : ('\r')? '\n' {skip();}
  ;
