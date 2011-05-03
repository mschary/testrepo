/*
 * Lua 5.1 grammar
 * 
 * Aasim Kumar Sokal
 * Konylabs
 *
 */
  
grammar KonyAPI;

options {
    backtrack = true; 
    memoize = true;
    output = AST;
    ASTLabelType = LuaAST;
}


tokens {

    // operators and other special chars
    
    AND                     = '&'               ;
    AND_ASSIGN              = '&='              ;
    ASSIGN                  = '='               ;
    AT                      = '@'               ;
    COLON                   = ':'               ;
    COMMA                   = ','               ;
    DEC                     = '--'              ;
    DIV                     = '/'               ;
    DIV_ASSIGN              = '/='              ;
    DOT                     = '.'               ;
    DOTSTAR                 = '.*'              ;
    EQUAL                   = '=='              ;
    GREATER_OR_EQUAL        = '>='              ;
    GREATER_THAN            = '>'               ;
    INC                     = '++'              ;
    LBRACK                  = '['               ;
    LCURLY                  = '{'               ;
    LESS_OR_EQUAL           = '<='              ;
    LESS_THAN               = '<'               ;
    LOGICAL_AND             = '&&'              ;
    LOGICAL_NOT             = '!'               ;
    LOGICAL_OR              = '||'              ;
    LPAREN                  = '('               ;
    MINUS                   = '-'               ;
    MINUS_ASSIGN            = '-='              ;
    MOD                     = '%'               ;
    MOD_ASSIGN              = '%='              ;
    NOT                     = '~'               ;
    NOT_EQUAL               = '!='              ;
    OR                      = '|'               ;
    OR_ASSIGN               = '|='              ;
    PLUS                    = '+'               ;
    PLUS_ASSIGN             = '+='              ;
    QUESTION                = '?'               ;
    RBRACK                  = ']'               ;
    RCURLY                  = '}'               ;
    RPAREN                  = ')'               ;
    SEMI                    = ';'               ;
    SHIFT_LEFT              = '<<'              ;
    SHIFT_LEFT_ASSIGN       = '<<='             ;
    SHIFT_RIGHT             = '>>'              ;
    SHIFT_RIGHT_ASSIGN      = '>>='             ;
    STAR                    = '*'               ;
    STAR_ASSIGN             = '*='              ;
    XOR                     = '^'               ;
    XOR_ASSIGN              = '^='              ;

    // keywords
    
    ASSERT                  = 'assert'          ;
    BOOLEAN                 = 'boolean'         ;
    BREAK                   = 'break'           ;
    CASE                    = 'case'            ;
    CHAR                    = 'char'            ;
    CONTINUE                = 'continue'        ;
    DEFAULT                 = 'default'         ;
    DO                      = 'do'              ;
    DOUBLE                  = 'double'          ;
    ELSE                    = 'else'            ;
    FALSE                   = 'false'           ;
    FLOAT                   = 'float'           ;
    FOR                     = 'for'             ;
    IF                      = 'if'              ;
    INT                     = 'int'             ;
    LONG                    = 'long'            ;
    NATIVE                  = 'native'          ;
    NEW                     = 'new'             ;
    NULL                    = 'null'            ;
    RETURN                  = 'return'          ;
    SHORT                   = 'short'           ;
    STATIC                  = 'static'          ;
    SWITCH                  = 'switch'          ;
    TRUE                    = 'true'            ;
    VOID                    = 'void'            ;
    WHILE                   = 'while'           ;
    
    // tokens for imaginary nodes
    
    ARGUMENT_LIST;
    ARRAY_DECLARATOR;
    ARRAY_DECLARATOR_LIST;
    ARRAY_ELEMENT_ACCESS;
    ARRAY_INITIALIZER;
    BLOCK_SCOPE;
    CAST_EXPR;
    EXPR;
    FOR_CONDITION;
    FOR_EACH;
    FOR_INIT;
    FOR_UPDATE;
    FORMAL_PARAM_LIST;
    FORMAL_PARAM_STD_DECL;
    FORMAL_PARAM_VARARG_DECL;
    FUNCTION_METHOD_DECL;
    LABELED_STATEMENT;
    LOCAL_MODIFIER_LIST;
    METHOD_CALL;
    MODIFIER_LIST;
    PARENTESIZED_EXPR;
    POST_DEC;
    POST_INC;
    PRE_DEC;
    PRE_INC;
    QUALIFIED_TYPE_IDENT;
    STATIC_ARRAY_CREATOR;
    SWITCH_BLOCK_LABEL_LIST;
    TYPE;
    UNARY_MINUS;
    UNARY_PLUS;
    VAR_DECLARATION;
    VAR_DECLARATOR;
    VAR_DECLARATOR_LIST;
    VOID_METHOD_DECL;
    IMPORT_DECL;
    STRUCTURE;
   
}

@header {
  package com.konylabs.transformer.api.grammer;
  import com.konylabs.transformer.scopes.*;
}

@lexer::header {
  package com.konylabs.transformer.api.grammer;
}

@lexer::members {
  public boolean preserveWhitespacesAndComments = true;
}

symbolSource: structDecl+ -> ^(BLOCK_SCOPE structDecl+)
	;

structDecl:
	IDENT ASSIGN  structBody  includesNode? SEMI? ->  ^(STRUCTURE IDENT structBody includesNode?)
	;
structBody:
		block 
	;

block
    :   LCURLY fieldDeclaratorList? RCURLY 
        ->  ^(BLOCK_SCOPE[$LCURLY, "BLOCK_SCOPE"] fieldDeclaratorList?)
    ;
	
typeNode:
	'--'! '[['! type ']]'! | '--'! type
	;
	
includesNode: 
  '--' 'includes' ASSIGN expression (COMMA expression)* -> ^(IMPORT_DECL expression+)
  ;
// Starting point for parsing a service file.
fieldDeclaratorList
    :   fieldDeclarator (COMMA fieldDeclarator)*
        ->  ^(VAR_DECLARATOR_LIST fieldDeclarator+)
    ;  

fieldDeclarator
@init{
 // System.out.println("Declaring field " + input.LT(1));
}
    :   IDENT (ASSIGN expression)? typeNode? 
        ->  ^(VAR_DECLARATOR typeNode? IDENT  expression?)
    ;
type
    :   simpleType
    |   objectType
    ;

simpleType // including static arrays of simple type elements
    :   primitiveType 
        ->  ^(TYPE primitiveType )  
    ;
    
objectType // including static arrays of object type reference elements
    :   qualifiedTypeIdent 
        ->  ^(TYPE qualifiedTypeIdent )
    ;

qualifiedTypeIdent
    :   IDENT (DOT IDENT)*
        ->  ^(QUALIFIED_TYPE_IDENT IDENT+) 
    ;

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

qualifiedIdentList
    :   qualifiedIdentifier (COMMA! qualifiedIdentifier)*
    ;
    
formalParameterList
    :   LPAREN 
        (   // Contains at least one standard argument declaration and optionally a variable argument declaration.
            (formalParameterStandardDecl (COMMA formalParameterStandardDecl)*)? 
            ->  ^(FORMAL_PARAM_LIST[$LPAREN, "FORMAL_PARAM_LIST"] formalParameterStandardDecl* ) 
        )
        RPAREN
    ;
    
formalParameterStandardDecl
    :   IDENT typeNode?
        ->  ^(FORMAL_PARAM_STD_DECL typeNode? IDENT)
    ;
        
qualifiedIdentifier
    :   (   IDENT               ->  IDENT
        )
        (   DOT ident=IDENT     ->  ^(DOT $qualifiedIdentifier $ident)
        )*
    ;
    
// STATEMENTS / BLOCKS

expression
    :   primaryExpression
        ->  ^(EXPR primaryExpression)
    ;
    
primaryExpression
    :   
    |   literal
    |   structBody 
    |   qualifiedIdentExpression
    |   (   primitiveType                               ->  primitiveType
        )
    ;
    
qualifiedIdentExpression
        // The qualified identifier itself is the starting point for this rule.
    :   (   qualifiedIdentifier                             ->  qualifiedIdentifier
        )
        // And now comes the stuff that may follow the qualified identifier.
        (   
        |   arguments                                   ->  ^(METHOD_CALL qualifiedIdentifier arguments)
        |   outerDot=DOT
        )?
    ;


arguments
    :   formalParameterList?
        ->  ^(ARGUMENT_LIST formalParameterList?)
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

// LEXER

HEX_LITERAL : '0' ('x'|'X') HEX_DIGIT+ INTEGER_TYPE_SUFFIX? ;

DECIMAL_LITERAL : ('0' | '1'..'9' '0'..'9'*) INTEGER_TYPE_SUFFIX? ;

OCTAL_LITERAL : '0' ('0'..'7')+ INTEGER_TYPE_SUFFIX? ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

fragment
INTEGER_TYPE_SUFFIX : ('l'|'L') ;

FLOATING_POINT_LITERAL
    :   ('0'..'9')+ 
        (
            DOT ('0'..'9')* EXPONENT? FLOAT_TYPE_SUFFIX?
        |   EXPONENT FLOAT_TYPE_SUFFIX?
        |   FLOAT_TYPE_SUFFIX
        )
    |   DOT ('0'..'9')+ EXPONENT? FLOAT_TYPE_SUFFIX?
    ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

fragment
FLOAT_TYPE_SUFFIX : ('f'|'F'|'d'|'D') ;

CHARACTER_LITERAL
    :   '\'' ( ESCAPE_SEQUENCE | ~('\''|'\\') ) '\''
    ;

STRING_LITERAL
    :  '"' ( ESCAPE_SEQUENCE | ~('\\'|'"') )* '"'
    ;

fragment
ESCAPE_SEQUENCE
    :   '\\' ('b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
    |   UNICODE_ESCAPE
    |   OCTAL_ESCAPE
    ;

fragment
OCTAL_ESCAPE
    :   '\\' ('0'..'3') ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7') ('0'..'7')
    |   '\\' ('0'..'7')
    ;

fragment
UNICODE_ESCAPE
    :   '\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
    ;
    
IDENT
    :   LETTER (LETTER | '0'..'9')*
    ;

fragment
LETTER  :   ('a'..'z' | 'A'..'Z' | '_')
    ;    
WS  :  (' '|'\r'|'\t'|'\u000C'|'\n') 
    {   
        if (!preserveWhitespacesAndComments) {
            skip();
        } else {
            $channel = HIDDEN;
        }
    }
    ;

COMMENT
    :   '/*' ( options {greedy=false;} : . )* '*/'
    {   
        if (!preserveWhitespacesAndComments) {
            skip();
        } else {
            $channel = HIDDEN;
        }
    }
    ;

LINE_COMMENT
    : '--//' ~('\n'|'\r')* '\r'? '\n'
    {   
        if (!preserveWhitespacesAndComments) {
            skip();
        } else {
            $channel = HIDDEN;
        }
    }
    ;