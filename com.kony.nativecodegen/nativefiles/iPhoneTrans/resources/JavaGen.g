tree grammar JavaGen;

options {
  output = template;
  tokenVocab = Lua;
  ASTLabelType = LuaAST;
  backtrack = true;
  k = 2;
}

@header {
  package com.konylabs.transformer.lua.grammer;
  import com.konylabs.transformer.scopes.*;
  import org.antlr.stringtemplate.*;
  import java.util.Hashtable;
  import java.util.LinkedHashMap;
}
 
@members {
    SymbolTable symtab;
    public boolean hadErrors = false;

    public ArrayList<String> functions = new ArrayList<String>();
    public ArrayList<String> vars = new ArrayList<String>();
    
    Map<Integer,String> commentMap = null;
    String fileName = null;

    int tmpnum = 0;
    Scope currentScope = null;

    Scope fileScope = null;
    public JavaGen(String fileName, TreeNodeStream input, SymbolTable symtab, final Map<Integer,String> commentMap) {
        this(input);
        this.symtab = symtab;
        this.commentMap = commentMap;
        this.fileName = fileName;
        fileScope = symtab.fileScope;
        currentScope = fileScope;
    }

    String getNewTmpName()
    {
        return ("tmp" + (++tmpnum));
    }

//    Scope getFileScope(Scope now){
//      if(now != null){
//        now = now.getEnclosingScope();
//      }
//    }

     String getPrefix(String prefix){
      String ret = null;
      if(prefix != null && !prefix.equals("local")){
        ret = prefix;
      }
      return ret;
    }
    String getJavaType(Type type, boolean isMethodReturn)
    {
        if ((type == null) || (type == SymbolTable._null) || 
            (!isMethodReturn && (type == SymbolTable._void)))
        {
            return ("Object");
        }
        if ((type instanceof StructSymbol) || (type.toString().equals("LuaTable")))
        {
            return "com.konylabs.vm.LuaTable";
        }
        return type.toString();
    }

    String getJavaType(Type type)
    {
        return getJavaType(type, false);
    }

    String getJavaObject(LuaAST node, String val)
    {
        Type type = node.evalType;
        if ((type == null) || (type == SymbolTable._null) || (val.equals("null")))
        {
            return ("LuaNil.nil");
        }
        if (type.getTypeIndex() == SymbolTable.tBOOLEAN) {
            if (val.equals("true")) {
                return ("Boolean.TRUE");
            }
            else if (val.equals("false")) {
                return ("Boolean.FALSE");
            }
        }
        if ((type.getTypeIndex() == SymbolTable.tINT) ||
            (type.getTypeIndex() == SymbolTable.tFLOAT)) {
            return ("new Double(" + val + ")");
        }
        if ((node.symbol != null) && (node.symbol instanceof MethodSymbol)) {
            return ("new EventHandler() { public void execute(KonyWidget widget) {" +
                              val + "(); } }");
        }
        return (val);
    }
}


program[String fileName]
@after{
  StringTemplate code = $st;
  $st = templateLib.getInstanceOf("comment2_tmpl");
  String comment = commentMap.get(0);
  $st.setAttribute("prevComment",comment); 
  $st.setAttribute("stmt",code);
}    :   translationUnit
        -> program_tmpl(fileName={fileName}, vars={vars},functions={functions} )
    ;

translationUnit
: ^(LUA_SOURCE (d+=blockStatement)+)
  -> file(defs={$d})
    ;

blockStatement
@after{
  StringTemplate code = $st;
  $st = templateLib.getInstanceOf("comment2_tmpl");
  $st.setAttribute("prevComment",commentMap.get($start.getTokenStartIndex()-1)); 
  $st.setAttribute("stmt",code); 
  $st.setAttribute("postComment",commentMap.get($start.getTokenStopIndex()+1)); 
  commentMap.remove($start.getTokenStartIndex()-1);
  commentMap.remove($start.getTokenStopIndex()+1);
} 
  : variableDeclaration            -> {$variableDeclaration.st}
  | statement    ->  {$statement.st}
 ;
  

block
: ^(BLOCK_SCOPE blockStmts)           ->   {$blockStmts.st} ;


blockStmts
:
(bs+=blockStatement)+    ->       block_tmpl(stmt={$bs});


statement 
:  
      block                       ->    {$block.st}
  |   expression                  ->    {$expression.st}
  |   methodCallStmt              ->    {$methodCallStmt.st}
  |   whileStatement              ->    {$whileStatement.st}
  |   repeatStatement             ->    {$repeatStatement.st}
  |   returnStarement             ->    {$returnStarement.st}
  |   ifStatement                 ->    {$ifStatement.st}
  |   forStatement                ->    {$forStatement.st}
  |   forInStatement              ->    {$forInStatement.st}
  |   methodDeclaration           ->    {$methodDeclaration.st}
  |   breakStmt                   ->    {$breakStmt.st}
    | assignStmt                  ->    {$assignStmt.st}    
  ;

methodCallStmt
:
m=methodCall   ->    methodCallStmt_tmpl(call={$m.st})
;


breakStmt
:(BREAK        -> break_tmpl());
 
whileStatement
: ^(WHILE a=expression b=statement)               ->    while_tmpl(cond={$a.st}, stmt={$b.st});

repeatStatement
: ^(REPEAT b=statement UNTIL a=expression)       ->    doWhile_tmpl(cond={$a.st}, stmt={$b.st});

//returnStarement:   ^(RETURN val=expression?)                    ->    return_tmpl(val={$val.st});
returnStarement
:   ^(RETURN val=expression? expression*)          ->    return_tmpl(val={$val.st});
  
ifStatement
 :
  ^(IF cond=expression thenBlock=statement elb=ifStatement)      ->    if_stmt(cond={$cond.st}, stmt={$thenBlock.st}, elb={$elb.st})
  | ^(IF cond=expression thenBlock=statement  elseBlock=statement)      ->    ifelse_stmt(cond={$cond.st}, stmt={$thenBlock.st}, elseb={$elseBlock.st})
  | ^(IF cond=expression thenBlock=statement)                       ->    if_stmt1(cond={$cond.st}, stmt={$thenBlock.st})
;

forInStatement
: forIpairStatement -> {$forIpairStatement.st}| forPairStatement -> {$forPairStatement.st};

forIpairStatement
: ^(FOR_EACH idx=name val=name ^(FOR_IN_CONDITION ^(EXPR ^(METHOD_CALL met=name ^(ARGUMENT_LIST ^(EXPR var=name))))) ^(BLOCK_SCOPE (bl+=blockStatement)+))
  -> forIpair_tmpl(type={"int"}, val={$idx.st}, type1={getJavaType($val.start.evalType)}, val1={$val.st}, met={$met.st}, type2={getJavaType($var.start.evalType)}, var={$var.st}, bl={$bl} )
;

forPairStatement: ^(FOR_EACH idx=name ^(FOR_IN_CONDITION ^(EXPR ^(METHOD_CALL met=name ^(ARGUMENT_LIST ^(EXPR var=name))))) ^(BLOCK_SCOPE (bl+=blockStatement)+))
  -> forPair_tmpl(type={getJavaType($idx.start.evalType)}, val={$idx.st}, met={$met.st}, type1={getJavaType($var.start.evalType)}, var={$var.st}, bl={$bl} )
;


forStatement
: ^(FOR init=forInit cond=forCondition[$init.node] updt=forUpdater[$init.node]? stmt=block){
  
}  
            ->  for_tmpl(init={$init.st}, cond={$cond.st}, updt={$updt.st}, stmt={$stmt.st});

forInit returns[LuaAST node]
@after{
  StringTemplate code = $st;
  $st = templateLib.getInstanceOf("comment2_tmpl");
  String comment1 = commentMap.get($start.getTokenStartIndex()-1);
  String comment2 = commentMap.get($start.getTokenStopIndex()+1);
  if(comment1 != null && comment1.startsWith("//")){
      comment1 = "/*" + comment1.substring(2,comment1.length()) + "*/";
  }
  if(comment2 != null && comment2.startsWith("//")){
      comment2 = "/*" + comment2.substring(2,comment2.length()) + "*/";
  }
  
  $st.setAttribute("prevComment",comment1); 
  $st.setAttribute("stmt",code); 
  $st.setAttribute("postComment",comment2); 
  commentMap.remove($start.getTokenStartIndex()-1);
  commentMap.remove($start.getTokenStopIndex()+1);
}: ^(FOR_INIT IDENT e=expression){$node = $IDENT;}             ->  forInit_tmpl(type={getJavaType($IDENT.evalType)}, var={$IDENT.text} , init={$e.st})
;
forCondition[LuaAST name]
@after{
  StringTemplate code = $st;
  $st = templateLib.getInstanceOf("comment2_tmpl");
  String comment1 = commentMap.get($start.getTokenStartIndex()-1);
  String comment2 = commentMap.get($start.getTokenStopIndex()+1);
  if(comment1 != null && comment1.startsWith("//")){
      comment1 = "/*" + comment1.substring(2,comment1.length()) + "*/";
  }
  if(comment2 != null && comment2.startsWith("//")){
      comment2 = "/*" + comment2.substring(2,comment2.length()) + "*/";
  }
  
  $st.setAttribute("prevComment",comment1); 
  $st.setAttribute("stmt",code); 
  $st.setAttribute("postComment",comment2); 
  commentMap.remove($start.getTokenStartIndex()-1);
  commentMap.remove($start.getTokenStopIndex()+1);
}:^(FOR_CONDITION cond=expression)    ->  forCondition_tmpl(name={$name.getText()}, cond={$cond.st})
;
    
forUpdater[LuaAST name]
@after{
  StringTemplate code = $st;
  $st = templateLib.getInstanceOf("comment2_tmpl");
  String comment1 = commentMap.get($start.getTokenStartIndex()-1);
  String comment2 = commentMap.get($start.getTokenStopIndex()+1);
  if(comment1 != null && comment1.startsWith("//")){
      comment1 = "/*" + comment1.substring(2,comment1.length()) + "*/";
  }
  if(comment2 != null && comment2.startsWith("//")){
      comment2 = "/*" + comment2.substring(2,comment2.length()) + "*/";
  }
  
  $st.setAttribute("prevComment",comment1); 
  $st.setAttribute("stmt",code); 
  $st.setAttribute("postComment",comment2); 
  commentMap.remove($start.getTokenStartIndex()-1);
  commentMap.remove($start.getTokenStopIndex()+1);
}:^(FOR_UPDATE cond=expression)         ->  forUpdater_tmpl(name={$name.getText()}, cond={$cond.st})
;

qualifiedDecl
@init{
  String prefix = null;
}
:qualifiedMember{
      if (($start.symbol != null) && !fileName.equals($start.symbol.def.declaredIn)) {
          prefix = $start.symbol.def.declaredIn;
      } else if (!fileName.equals($start.declaredIn)) {
          prefix = $start.declaredIn;
      }
}   -> qualifiedDecl_tmpl(prefix={prefix}, mem={$qualifiedMember.st})
; 
qualifiedMember
@init {
    boolean docast = false;
    Type cast = null;
}
      : ^(DOT a=qualifiedMember name ) {
           if (!($a.start.evalType instanceof StructSymbol)) {
               docast = true;
           }
           $start.evalType = $name.start.evalType;
           cast = $start.getPromoteToType();
           $st = templateLib.getInstanceOf("dotMember_tmpl");
           if (!($a.start.evalType instanceof StructSymbol)) {
               $st.setAttribute("mem", "((LuaTable) " + $a.st + ")");
           }
           else {
               $st.setAttribute("mem", $a.st);
           }
           $st.setAttribute("name", $name.st);
           $st.setAttribute("cast", cast);
        }
      | ^(COLON a=qualifiedMember name ){$start.evalType = $name.start.evalType;}
             -> dotMember_tmpl(mem={$a.st}, name={$name.st})
      | name -> {$name.st}
      | ^(QUALIFIED_TYPE_IDENT (n+=name)+) {$start.evalType = ((LuaAST)n.getStart()).evalType;}
             -> dotList_tmpl(names={$n}) 
      | methodCall -> {$methodCall.st}
      | arrayAccess -> {$arrayAccess.st}
;

methodQualifiedMember
      : ^(DOT a=methodQualifiedMember name ) {$start.evalType = $name.start.evalType;}
             -> method_dotMember_tmpl(mem={$a.st}, name={$name.st})
      | ^(COLON a=methodQualifiedMember name ){$start.evalType = $name.start.evalType;}
             -> method_dotMember_tmpl(mem={$a.st}, name={$name.st})
      | name -> {$name.st}
      | ^(QUALIFIED_TYPE_IDENT (n+=name)+) {$start.evalType = ((LuaAST)n.getStart()).evalType;}
             -> dotList_tmpl(names={$n})
      | methodCall -> {$methodCall.st}
      | arrayAccess -> {$arrayAccess.st}
;

name : IDENT {$name.start.evalType = $IDENT.evalType;} -> ident_tmpl(name={$IDENT.text});

methodDeclaration
@init{
  boolean isLocal = false;
}
@after{
  StringTemplate code = $st;
  $st = templateLib.getInstanceOf("comment2_tmpl");
  $st.setAttribute("prevComment",commentMap.get($start.getTokenStartIndex()-1)); 
  $st.setAttribute("stmt",code); 
  $st.setAttribute("postComment",commentMap.get($start.getTokenStopIndex()+1)); 
  commentMap.remove($start.getTokenStartIndex()-1);
  commentMap.remove($start.getTokenStopIndex()+1);
  functions.add($st.toString());
}:^(FUNCTION_METHOD_DECL mod=LOCAL? a=methodQualifiedMember b=formalParameterList block)
{ 
    if(mod != null){
        isLocal = true;
    }
}
   ->   methodDecl_tmpl(isLocal={isLocal}, type={getJavaType($a.start.evalType, true)}, name={$a.st},params={$b.st}, bl={$block.st});

formalParameterList
:
            ^(FORMAL_PARAM_LIST (par+=formalParameterStandardDecl)+ )   ->  paramList_tmpl(params={$par})
        |   (FORMAL_PARAM_LIST)                                         ->  paramList_tmpl()
    ;
formalParameterStandardDecl
@after{
  StringTemplate code = $st;
  $st = templateLib.getInstanceOf("comment2_tmpl");
  String comment1 = commentMap.get($start.getTokenStartIndex()-1);
  String comment2 = commentMap.get($start.getTokenStopIndex()+1);
  if(comment1 != null && comment1.startsWith("//")){
      comment1 = "/*" + comment1.substring(2,comment1.length()) + "*/";
  }
  if(comment2 != null && comment2.startsWith("//")){
      comment2 = "/*" + comment2.substring(2,comment2.length()) + "*/";
  }
  
  $st.setAttribute("prevComment",comment1); 
  $st.setAttribute("stmt",code); 
  $st.setAttribute("postComment",comment2); 
  commentMap.remove($start.getTokenStartIndex()-1);
  commentMap.remove($start.getTokenStopIndex()+1);
}:^(FORMAL_PARAM_STD_DECL IDENT)          ->    param_tmpl(type={getJavaType($IDENT.symbol.type)}, name={$IDENT.text});

variableDeclaration
@init{
  ArrayList rList = new ArrayList();
  ArrayList eList = new ArrayList();
  $st = templateLib.getInstanceOf("varList_tmpl");
  boolean isLocal = true;
}
: 
^(VAR_DECLARATION ^(VAR_DECLARATOR_LIST (^(VAR_DECLARATOR mod=LOCAL? ident+=IDENT))+) (^(EXPR(e=expr{rList.add(e.st);eList.add(e.getStart());})+))*){
    java.util.Iterator lItr = $ident.iterator();
    java.util.Iterator rItr = rList.iterator();
    java.util.Iterator eItr = eList.iterator();
    if (mod == null) {
        isLocal = false;
    }
    while (lItr.hasNext()) {
        LuaAST lNode = (LuaAST) lItr.next();
        StringTemplate rNode = null;
        if (rItr.hasNext()) rNode = (StringTemplate) rItr.next();
        LuaAST eNode = null;
        if (eItr.hasNext()) eNode = (LuaAST) eItr.next();
        StringTemplate code = null;
        StringTemplate code1 = null;
        String prefix = null;
        if(!fileName.equals(lNode.declaredIn)){
            prefix = lNode.declaredIn;
        }
        boolean declared = false;
        if(lNode.isDeclaration){
            if(isLocal){
                code = templateLib.getInstanceOf("var_tmpl");
                //code.setAttribute("type", getJavaType(lNode.evalType));
                code.setAttribute("type", lNode.type);
                String vname = lNode.getText();
                code.setAttribute("prefix",prefix);
                code.setAttribute("name", vname);
                code.setAttribute("init", rNode);
                if ((eNode != null) && (eNode.getPromoteToType() != null)) {
                     code.setAttribute("ptype", eNode.getPromoteToType());
                }
                $st.setAttribute("vars", code);
                declared = true;
            }else{
				code = templateLib.getInstanceOf("varDecl_tmpl");
				code.setAttribute("isGlobal", !isLocal);
				code.setAttribute("type", getJavaType(lNode.evalType));
				String vname = lNode.getText();
                code.setAttribute("prefix",prefix);
                code.setAttribute("name", vname);
                vars.add(code.toString());
            }
        }
        
        if(!declared){ 
		        code1 = templateLib.getInstanceOf("assign_tmpl");
		        code1.setAttribute("isLocal", isLocal);
		        code1.setAttribute("prefix",prefix);
		        code1.setAttribute("a",lNode.getText());
		        code1.setAttribute("b",rNode);
                if ((eNode != null) && (eNode.getPromoteToType() != null)) {
                     code1.setAttribute("ptype", eNode.getPromoteToType());
                }
		        $st.setAttribute("vars", code1);
        }
    }
} 
;

assignStmt
@init {
    String jobj = "";
}
    : ^(VAR_DECLARATION ^(VAR_DECLARATOR_LIST ^(VAR_DECLARATOR ^(DOT q=qualifiedMember n=name))) ^(EXPR e=expr)) {
          jobj = getJavaObject($e.start, $e.st.toString());
      } -> field2_tmpl(tname={$q.st}, fname={$n.st}, expr={jobj})
    | ^(VAR_DECLARATION ^(VAR_DECLARATOR_LIST ^(VAR_DECLARATOR ^(ARRAY_ELEMENT_ACCESS a=primaryExpression b=expression))) ^(EXPR e=expr)) {
          jobj = getJavaObject($e.start, $e.st.toString());
        } -> field3_tmpl(tname={$a.st}, expr1={$b.st}, expr2={jobj})
;

expression 
: ^(EXPR expr)                      ->  {$expr.st};

expr :
        ^(LOGICAL_OR a=expr b=expr)              ->  bop_tmpl(op={"||"}, a={$a.st}, b={$b.st})
    |   ^(LOGICAL_AND a=expr b=expr)              ->  bop_tmpl(op={"&&"}, a={$a.st}, b={$b.st})

    |   ^(EQUAL a=expr b=expr)                    ->  bop_tmpl(op={"=="}, a={$a.st}, b={$b.st})
    |   ^(NOT_EQUAL a=expr b=expr)                ->  bop_tmpl(op={"!="}, a={$a.st}, b={$b.st})

    |   ^(LESS_OR_EQUAL a=expr b=expr)            ->  bop_tmpl(op={"<="}, a={$a.st}, b={$b.st}) 
    |   ^(GREATER_OR_EQUAL a=expr b=expr)         ->  bop_tmpl(op={">="}, a={$a.st}, b={$b.st}) 
    |   ^(GREATER_THAN  a=expr b=expr)            ->  bop_tmpl(op={">"}, a={$a.st}, b={$b.st})
    |   ^(LESS_THAN a=expr b=expr)                ->  bop_tmpl(op={"<"}, a={$a.st}, b={$b.st})

    |   ^(PLUS a=expr b=expr)                     ->  bop_tmpl(op={"+"}, a={$a.st}, b={$b.st}) 
    |   ^(MINUS a=expr b=expr)                    ->  bop_tmpl(op={"-"}, a={$a.st}, b={$b.st}) 
    |   ^(STAR a=expr b=expr)                     ->  bop_tmpl(op={"*"}, a={$a.st}, b={$b.st}) 
    |   ^(DIV a=expr b=expr)                      ->  bop_tmpl(op={"/"}, a={$a.st}, b={$b.st}) 
    |   ^(MOD a=expr b=expr)                      ->  bop_tmpl(op={"\%"}, a={$a.st}, b={$b.st}) 
    |   ^(EXPO a=expr b=expr)                     ->  pow_tmpl(a={$a.st}, b={$b.st})

    |   ^(CONCAT_STR a=expr b=expr)               ->  bop_tmpl(op={"+"}, a={$a.st}, b={$b.st})
    |   ^(UNARY_MINUS a=expr)                     ->  uop_tmpl(op={"-"}, a={$a.st})
    |   ^(UNARY_HASH a=expr)                      ->  hash_tmpl(a={$a.st})
    |   ^(NOT a=expr)                             ->  uop_tmpl(op={"!"}, a={$a.st})

    |   primaryExpression                         ->  {$primaryExpression.st}
    ;    

primaryExpression
:
       parenthesizedExpression                    ->    {$parenthesizedExpression.st}
    |   methodCall                                ->    {$methodCall.st}
    |   qualifiedDecl                             ->    {$qualifiedDecl.st}
    |   arrayAccess                               ->    {$arrayAccess.st}
    |   literal                                   ->    {$literal.st}//expr_tmpl(expr={$literal.text})
    |   tableconstructor                          ->    {$tableconstructor.st}
    ;

arrayAccess:^(ARRAY_ELEMENT_ACCESS a=primaryExpression b=expression)  
-> arrAccess_tmpl(name={$a.st}, index={$b.st});

methodCall
@init{
    String prefix = null;
    ArrayList rList = new ArrayList();
    ArrayList newstlist = new ArrayList();
}
//:^(METHOD_CALL a=methodQualifiedMember ^(ARGUMENT_LIST ((e+=expression)+)?)){
:^(METHOD_CALL a=methodQualifiedMember ^(ARGUMENT_LIST ((e+=expression){rList.add(e.getStart());})* )) {
      if($a.start.symbol != null){
          prefix = $a.start.symbol.def.declaredIn;
      }else{
          prefix = $a.start.declaredIn;
      }
    String name = ($a.st).toString();
    StringTemplate st1 = templateLib.getInstanceOf("javaapi_tmpl");
    st1.setAttribute("name", name);
    String newName = st1.toString();
    if (newName.equals(name)) {
        $st = templateLib.getInstanceOf("methodCall1_tmpl");
        $st.setAttribute("prefix", prefix);
        $st.setAttribute("name", $a.st);
        $st.setAttribute("args", $e);
    }
    else {
        if (rList.size() > 0) {
            java.util.Iterator st_itr = $e.iterator();
            java.util.Iterator ast_itr = rList.iterator();
            while (ast_itr.hasNext()) {
                 boolean addednewst = false;
                 LuaAST node = (LuaAST) ast_itr.next();
                 LuaAST cnode = (LuaAST) node.getChild(0);
                 StringTemplate stmpl = (StringTemplate) st_itr.next();
                 if (cnode.evalType != null) {
                      String newVal = getJavaObject(cnode, stmpl.toString());
                      if (!newVal.equals(stmpl.toString())) {
                           newstlist.add(new StringTemplate(newVal));
                           addednewst = true;
                      }
                 }
                 if (!addednewst) {
                     newstlist.add(stmpl);
                 }
            }
        }
        $st = templateLib.getInstanceOf("methodCall2_tmpl");
        $st.setAttribute("name", newName);
        $st.setAttribute("args", newstlist);
    }
}
;
arguments
    :   ^(ARGUMENT_LIST expressionList?)          ->  {$expressionList.st}
    ;
tableconstructor
@init {
     String tfname = getNewTmpName();
     String tname = "t";
     List<StringTemplate> params = new ArrayList<StringTemplate>();
     List<StringTemplate> paramNames = new ArrayList<StringTemplate>();
}
    : ^(ARRAY_INITIALIZER fieldlist[tname, params, paramNames]?) {
          StringTemplate code = templateLib.getInstanceOf("table_func_tmpl");
          code.setAttribute("fname", tfname);
          code.setAttribute("tname", tname);
          code.setAttribute("body", $fieldlist.st);
          code.setAttribute("params",params);
          functions.add(code.toString());
          System.out.println("Scope in tbl " + $start.scope);
          currentScope = $start.scope;
      } -> table_init_tmpl(fname={tfname}, params={paramNames})
    ;

fieldlist[String tname, List<StringTemplate> params, List<StringTemplate> paramNames]
     : ^(FIELD_LIST (f+=field[tname, params, paramNames])+) -> tblFieldList_tmpl(field={$f})
     ;

field[String tname, List<StringTemplate> params, List<StringTemplate> paramNames]
@init {
     String jobj = "";
}
     : ^(ARRAY_ELEMENT_ACCESS e1=expression e2=expression ){
           jobj = getJavaObject($e2.start, $e2.st.toString());
        } -> field3_tmpl(tname={$tname}, expr1={$e1.st}, expr2={jobj})
      | ^(VAR_DECLARATOR IDENT e=expression)  {
           jobj = getJavaObject($e.start, $e.st.toString());
        } -> field2_tmpl(tname={$tname}, fname={$IDENT.text}, expr={jobj})
      | ^(FIELD e=expression){
          LuaAST node = (LuaAST) e.start;
          int ty = node.getChild(0).getType();
          if(ty >= INT_LITERAL && ty <= LONGSTRING_LITERAL){ 
              System.out.println("Literal added  " + e);
		          StringTemplate code = templateLib.getInstanceOf("field1_tmpl");
		          code.setAttribute("tname", tname);
              code.setAttribute("expr", $e.st);
              //$st = code;
          }else{
              System.out.println("Some ident added " + e);
              Symbol s = currentScope.resolve(node.getChild(0).getText());
              if(s != null){
                  node.type = s.type;
              }
              StringTemplate code = templateLib.getInstanceOf("tbl_param_tmpl");
              code.setAttribute("type", node.type);
              code.setAttribute("name", $e.st);
              params.add(code);
              paramNames.add($e.st);
          }
      } ->  field1_tmpl(tname={$tname}, expr={$e.st})
      ;
      

parenthesizedExpression 
    :   ^(PARENTESIZED_EXPR expression)           ->  {$expression.st}        
    ;

expressionList: (e+=expression)+                     -> exprList_tmpl(expr={$e});

literal
@after {$st = %{$text};}
  : 'nil'   { $start.getToken().setText("null"); }
  | 'true'   { $start.getToken().setText("true"); }       
  | 'false' { $start.getToken().setText("false"); }       
  | string
  | number
  | floatLit
  | ELLIPSIS 
;


number : INT_LITERAL | HEX_LITERAL;
floatLit: FLOAT_LITERAL | EXP_LITERAL;
string  : NORMALSTRING_LITERAL | CHARSTRING_LITERAL | LONGSTRING_LITERAL;
