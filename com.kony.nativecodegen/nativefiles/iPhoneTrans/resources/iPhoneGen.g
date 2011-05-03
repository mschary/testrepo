tree grammar iPhoneGen;

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

    public java.util.Set<String> imports = new java.util.HashSet<String>();

    public Map<String, StringTemplate> localFunctions = new HashMap<String,StringTemplate>();
    public Map<String, StringTemplate> globalFunctions = new HashMap<String,StringTemplate>();

    public Map<String, StringTemplate> functionDefs = new HashMap<String,StringTemplate>();
    public Map<String, StringTemplate> globalFunctionDefs = new HashMap<String,StringTemplate>();

    public Map<String, StringTemplate> globalVars = new HashMap<String,StringTemplate>();
    public Map<String, StringTemplate> localVars = new HashMap<String,StringTemplate>();

    public Map<String, StringTemplate> declInits = new HashMap<String,StringTemplate>();

    public StructSymbol globalApi = null;

    Map<Integer,String> commentMap = null;
    String fileName = null;

    int tmpnum = 0;
    Scope currentScope = null;
    Type CallbackPtr = null;
    Scope fileScope = null;
    public iPhoneGen(String fileName, TreeNodeStream input, SymbolTable symtab, final Map<Integer,String> commentMap) {
        this(input);
        this.symtab = symtab;
        this.commentMap = commentMap;
        this.fileName = fileName;
        fileScope = symtab.fileScope;
        currentScope = fileScope;
        Symbol ss = symtab.globals.resolve("globalapi");
        if(ss != null){
            globalApi = (StructSymbol) ss.type;
        }
        Type st = (Type) symtab.globals.resolve("CallbackPtr");
        if(st != null){
          CallbackPtr = st;
        }else{
          CallbackPtr = new BuiltInTypeSymbol("CallbackPtr",SymbolTable.tUSER);
          symtab.globals.define((Symbol) CallbackPtr);
        }
    }

    String getNewTmpName()
    {
        return ("tmp" + (++tmpnum));
    }

}
 
program[String fileName] returns [StringTemplate headerFile]
@init{
  StringTemplate code = $st;
  $st = templateLib.getInstanceOf("comment2_tmpl");
  String comment = commentMap.get(0);
  $st.setAttribute("prevComment",comment); 
  $st.setAttribute("stmt",code);
}
@after{
  StringTemplate codeTmp = $st;
	  if(localFunctions.isEmpty() && !globalVars.isEmpty() && !fileName.equals("GlobalsExtern")){
	      Map m = $st.getAttributes();
	      m.remove("gvars");
	      java.util.Set<String> keys = globalVars.keySet();
	      for(String key : keys){
	         StringTemplate rhs = declInits.get(key);
//	         if(rhs != null){
			         StringTemplate tmplt = globalVars.get(key);
			         StringTemplate ntmplt = templateLib.getInstanceOf("varGl_tmpl");
			         ntmplt.setAttribute("type",tmplt.getAttribute("type"));
			         ntmplt.setAttribute("name",tmplt.getAttribute("name"));
			         ntmplt.setAttribute("init",declInits.get(key));
			         $st.setAttribute("gvars",ntmplt);
//	         }
	      }
	  }
}
    :   translationUnit{
          if(!fileName.equals("GlobalsExtern")){
		          $headerFile = templateLib.getInstanceOf("headerFile_tmpl");
		          $headerFile.setAttribute("fileName",fileName);
		          //$headerFile.setAttribute("imports",imports);
		          $headerFile.setAttribute("lfunctions",functionDefs);
              $headerFile.setAttribute("gfunctions",globalFunctionDefs);
		      }else{
              $headerFile = templateLib.getInstanceOf("globalHeaderFile_tmpl");
              $headerFile.setAttribute("globalVars",globalVars);
              $headerFile.setAttribute("globalFunctionDefs",functionDefs);
		      }
          localVars.clear();
          System.err.println("FILE ::;:::: " + fileName);
      }
//        -> classFile_tmpl(fileName={fileName},imports={imports}, vars={localVars}, functions={localFunctions}, gfunctions={globalFunctions} )
        -> classFile_tmpl(fileName={fileName}, imports={imports},vars={localVars}, gvars={globalVars}, functions={localFunctions}, gfunctions={globalFunctions} )
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
  : variableDeclaration{
    StringTemplate tmplt = $variableDeclaration.st;
  }            -> {$variableDeclaration.st}
  | statement    ->  {$statement.st}
 ;
  

block
: ^(BLOCK_SCOPE blockStmts?)           ->   {$blockStmts.st} ;


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
  -> forIpair_tmpl(type={"int"}, val={$idx.st}, type1={($val.start.evalType)}, val1={$val.st}, met={$met.st}, type2={($var.start.evalType)}, var={$var.st}, bl={$bl} )
;

forPairStatement: ^(FOR_EACH idx=name ^(FOR_IN_CONDITION ^(EXPR ^(METHOD_CALL met=name ^(ARGUMENT_LIST ^(EXPR var=name))))) ^(BLOCK_SCOPE (bl+=blockStatement)+))
  -> forPair_tmpl(type={($idx.start.evalType)}, val={$idx.st}, met={$met.st}, type1={($var.start.evalType)}, var={$var.st}, bl={$bl} )
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
}: ^(FOR_INIT IDENT e=expression){$node = $IDENT;}             ->  forInit_tmpl(type={($IDENT.evalType)}, var={$IDENT.text} , init={$e.st})
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
          //prefix = $start.symbol.def.declaredIn;
      } else if (!fileName.equals($start.declaredIn)) {
          //prefix = $start.declaredIn;
      }
      //Aasim
      StringTemplate tmplt = $qualifiedMember.st;
                    List<StringTemplate> nList = new ArrayList<StringTemplate>();
                     while(tmplt != null){
                       nList.add(tmplt);
                         tmplt = (StringTemplate)tmplt.getAttributes().get("mem");
                     }
                     StringTemplate last = tmplt;
      
}   -> qualifiedDecl_tmpl(prefix={prefix}, mem={$qualifiedMember.st})
; 
qualifiedMember
@init {
    boolean docast = false;
    Type cast = null;
}
      : ^(DOT a=qualifiedMember name ) {
          StringTemplate iPhoneProp = null;
           if (!($a.start.evalType instanceof StructSymbol)) {
               docast = true;
           }
           System.out.println("Member " + $a.st + " with name " + $name.st);
           $start.evalType = $name.start.evalType;
           $st = templateLib.getInstanceOf("dotMember_tmpl");
           $st.setAttribute("mem", $a.st);
           $st.setAttribute("name", $name.st);
        }
      | ^(COLON a=qualifiedMember name ){$start.evalType = $name.start.evalType;}
             -> dotMember_tmpl(mem={$a.st}, name={$name.st})
      | name -> {$name.st}
      | ^(QUALIFIED_TYPE_IDENT (n+=name)+) {$start.evalType = ((LuaAST)n.getStart()).evalType;}
             -> dotList_tmpl(names={$n}) 
      | methodCall -> {$methodCall.st}
      | arrayAccess -> {$arrayAccess.st}
;

methodQualifiedMember returns [Type isCallback]
     : ^(DOT a=methodQualifiedMember name ) {$start.evalType = $name.start.evalType;}
             -> method_dotMember_tmpl(mem={$a.st}, name={$name.st})
      | ^(COLON a=methodQualifiedMember name ){$start.evalType = $name.start.evalType;}
             -> method_dotMember_tmpl(mem={$a.st}, name={$name.st})
      | name{
      $isCallback = $name.start.promoteToType;
      } -> {$name.st}
      | ^(QUALIFIED_TYPE_IDENT (n+=name)+) {$start.evalType = ((LuaAST)n.getStart()).evalType;}
             -> dotList_tmpl(names={$n})
      | methodCall -> {$methodCall.st}
      | arrayAccess -> {$arrayAccess.st}
;

name : IDENT {$name.start.evalType = $IDENT.evalType;
} -> ident_tmpl(name={$IDENT.text});

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

  if(isLocal)
    localFunctions.put(code.getAttribute("name").toString(), $st);
  else
    globalFunctions.put(code.getAttribute("name").toString(), $st);
  //$st = null;
}:^(FUNCTION_METHOD_DECL mod=LOCAL? a=methodQualifiedMember ^(FORMAL_PARAM_LIST ((par+=formalParameterStandardDecl)+ )?) block)
{ 
    //First check if this is in global
    if(mod != null){
        isLocal = true;
    }
        StringTemplate mtmpl = null;
        StringTemplate tmpl =  null;
        LuaAST tmpCld = ((LuaAST)$a.start.getChild(0));
        Type tType = (tmpCld != null)?tmpCld.promoteToType : SymbolTable._void;
        if(tType == CallbackPtr || $a.start.promoteToType == CallbackPtr || $a.start.evalType == CallbackPtr){
            mtmpl = templateLib.getInstanceOf("methodCallbackDecl_tmpl");
		        mtmpl.setAttribute("isLocal", isLocal);
		        mtmpl.setAttribute("type", $a.start.evalType);
		        mtmpl.setAttribute("name", $a.st);
		        MethodSymbol ms = (MethodSymbol) ((LuaAST)$a.start.getChild(0)).symbol;
		        List pars = new ArrayList();
		        if(ms != null){
		           int idx = 0;
		           for (String a1 : ms.getMembers().keySet()){
		              StringTemplate ptmpl = templateLib.getInstanceOf("callbackParams_tmpl");
		              ptmpl.setAttribute("lName", a1);
		              ptmpl.setAttribute("index",idx++);
                  //pars.add(ptmpl);
                  mtmpl.setAttribute("params", ptmpl);
		           }
		        }
		        //mtmpl.setAttribute("params", pars);
		        mtmpl.setAttribute("bl", $block.st);
	         tmpl =  templateLib.getInstanceOf("methodDeclGlobal_tmpl");
	         tmpl.setAttribute("type", $a.start.evalType);
	         tmpl.setAttribute("name", $a.st);

           StringTemplate par12 = templateLib.getInstanceOf("param_tmpl");
           StringTemplate par123 = templateLib.getInstanceOf("comment2_tmpl");
           par12.setAttribute("type","NSArray");
           par12.setAttribute("name","params");
           par123.setAttribute("stmt",par12);

           tmpl.setAttribute("params", par123);

        }else{
            mtmpl = templateLib.getInstanceOf("methodDecl_tmpl");
		        mtmpl.setAttribute("isLocal", isLocal);
		        mtmpl.setAttribute("type", $a.start.evalType);
		        mtmpl.setAttribute("name", $a.st);
		        mtmpl.setAttribute("params", $par);
		        mtmpl.setAttribute("bl", $block.st);
         tmpl =  templateLib.getInstanceOf("methodDeclGlobal_tmpl");
         tmpl.setAttribute("type", $a.start.evalType);
         tmpl.setAttribute("name", $a.st);
         tmpl.setAttribute("params", $par);
        }
        $st = mtmpl;        
        
	       currentScope = $a.start.scope;

	       if(!isLocal){
	         globalFunctionDefs.put($a.st.toString(), tmpl);
	       }else{
	         functionDefs.put($a.st.toString(), tmpl);
	       }
}
//   ->   methodDecl_tmpl(isLocal={isLocal}, type={$a.start.evalType}, name={$a.st}, params={$par}, bl={$block.st});
;
formalParameterList
:
            ^(FORMAL_PARAM_LIST (par+=formalParameterStandardDecl)+ )   ->  paramList_tmpl(params={$par})
        |   (FORMAL_PARAM_LIST)                                         ->  paramList_tmpl()
        |   (FORMAL_PARAM_LIST FORMAL_PARAM_VARARG_DECL)               ->  vararg_tmpl()
        
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
}:^(FORMAL_PARAM_STD_DECL IDENT){
        currentScope = $IDENT.scope;
}          ->    param_tmpl(type={$IDENT.symbol.type}, name={$IDENT.text});

variableDeclaration
@init{
  ArrayList rList = new ArrayList();
  ArrayList eList = new ArrayList();
  $st = templateLib.getInstanceOf("varList_tmpl");
  boolean isLocal = true;
  boolean isDeclaration = false;
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

        if(lNode.getText().equals("startupform")){
           System.err.println("VARDECL:::::: " + $ident + " :: " + eNode);
        }
        if(lNode.evalType == SymbolTable._void && lNode.symbol != null && lNode.evalType != lNode.symbol.type){
            lNode.evalType = lNode.symbol.type;
            lNode.type = lNode.symbol.type;
        }

        StringTemplate code = null;
        StringTemplate code1 = null;
        String prefix = null;
        if(!fileName.equals(lNode.declaredIn)){
            //prefix = lNode.declaredIn;
            if(prefix != null)
              imports.add(prefix + ".h");
        }
	       if(eNode != null && eNode.getChild(0) != null){
           //System.err.println("VARDECL::::::1 " + $ident + " :: " + eNode);
	         StringTemplate code12 = templateLib.getInstanceOf("checkAbstractTypeMap");
	         
	         code12.setAttribute("type",((LuaAST)eNode.getChild(0)).evalType);
	         if(code12.toString().length() > 1){
	             lNode.evalType = ((LuaAST)eNode.getChild(0)).evalType;
	             lNode.type = lNode.evalType;
	             if(lNode.symbol != null){
	                 lNode.symbol.def.type = lNode.evalType;
	                 lNode.symbol.type = lNode.evalType;
	             }
	         }
	       }
    System.err.println("VARDECL::::::2 " + $ident + " :: " + eNode);
//            if(eNode != null && eNode.evalType == SymbolTable._void){
//                eNode.evalType = lNode.evalType;
//                eNode.type = lNode.evalType;
//            }
	      boolean declared = false; 
        if(lNode.isDeclaration){
            isDeclaration = true;
            if(lNode.type == SymbolTable._void){
              lNode.type = eNode.evalType;
              lNode.evalType = eNode.evalType;
               if(lNode.symbol != null){
                   lNode.symbol.def.type = eNode.evalType;
                   lNode.symbol.type = eNode.evalType;
               }
            }
//            
//    System.err.println("VARDECL::::::3 " + $ident + " :: " + eNode);
            if(isLocal){
                code = templateLib.getInstanceOf("var_tmpl");
                code.setAttribute("type", lNode.type);
                String vname = lNode.getText();
                //code.setAttribute("prefix",prefix);
                code.setAttribute("name", vname);
                code.setAttribute("init", rNode);
                if ((eNode != null) && (eNode.getPromoteToType() != null)) {
                     code.setAttribute("ptype", eNode.getPromoteToType());
                }
                $st.setAttribute("vars", code);
                //System.err.println("LOCAL PUTTING " + code);
                localVars.put(vname, code);
                declared = true;
            }else{
                
								code = templateLib.getInstanceOf("varDecl_tmpl");
                code.setAttribute("type", (lNode.type));
								String vname = lNode.getText();
                //code.setAttribute("prefix",prefix);
                char testChar = rNode.toString().charAt(0);
                if(testChar == '@' || Character.isDigit(testChar))
                    declInits.put(vname,rNode);
                code.setAttribute("name", vname);
                //System.err.println("PUTTING " + code);
                globalVars.put(vname, code);
                declared = false;
            }
        }
//        
        if(!declared){ 
            if(lNode.isDeclaration){
                code = templateLib.getInstanceOf("var_tmpl");
                //code.setAttribute("type", lNode.type);
                String vname = lNode.getText();
                code.setAttribute("name", vname);
                code.setAttribute("init", rNode);
                $st.setAttribute("vars", code);
            }else{
		          code1 = templateLib.getInstanceOf("assign_tmpl");
	            code1.setAttribute("a",lNode.getText());
	            code1.setAttribute("b",rNode);
	            
	            $st.setAttribute("vars", code1);
		        }
		        //code1.setAttribute("prefix", prefix);
            System.err.println("VARDECL::::::4 " + $ident + " :: " + eNode + " ::: " + lNode.evalType);
            System.err.println("Assign Declaring is " + code1);
            Symbol nodeSymbol = lNode.scope.resolve(lNode.getText());
            if(nodeSymbol != null && nodeSymbol.type == SymbolTable._void){
              nodeSymbol.type = lNode.evalType;
              nodeSymbol.def.type = lNode.evalType;
              nodeSymbol.def.evalType = lNode.evalType;
              StringTemplate tmp123 = localVars.get(lNode.getText());
              if(tmp123 == null){
                tmp123 = globalVars.get(lNode.getText());
              }
              if(tmp123 != null){
               // tmp123.getAttributes().remove("type");
                tmp123.setAttribute("type", lNode.evalType);
                lNode.type = lNode.evalType;
                System.err.println("Replaced " +tmp123);
              }
            }
                System.err.println("VARDECL::::::5 " + $ident + " :: " + eNode + " ::: " + lNode.evalType);
        }
    }
}
;

assignStmt
@init {
  boolean isLocal = true;
  boolean isDeclaration = false;
}
    : ^(VAR_DECLARATION ^(VAR_DECLARATOR_LIST ^(VAR_DECLARATOR ^(DOT q=qualifiedMember n=name))) ^(EXPR e=expr)) {
          StringTemplate iPhoneProp = null;
          StringTemplate isForm = null;
          if($q.st.getAttributes().containsKey("mem"))
              isForm = (StringTemplate) $q.st.getAttribute("mem");
          if(isForm != null && isForm.toString().startsWith("form")){
             String tmpStr = isForm.toString();
             if(tmpStr.indexOf(".") > 0)
              tmpStr = tmpStr.substring(0,tmpStr.indexOf("."));
             iPhoneProp = templateLib.getInstanceOf("property_tmpl");
             iPhoneProp.setAttribute("fparam", tmpStr); 
             iPhoneProp.setAttribute("member", $q.st.getAttribute("name"));
             iPhoneProp.setAttribute("expr1", $n.st);
             iPhoneProp.setAttribute("expr2", $e.st);
             iPhoneProp.setAttribute("type", $e.start.evalType);
          }else{
             iPhoneProp = templateLib.getInstanceOf("field4_tmpl");
             iPhoneProp.setAttribute("tname", $q.st);
             iPhoneProp.setAttribute("fname", $n.st);
             iPhoneProp.setAttribute("expr", $e.st);
             iPhoneProp.setAttribute("type", $e.start.evalType);
          }
          $st = iPhoneProp;
      } //-> field4_tmpl(tname={$q.st}, fname={$n.st}, expr={$e.st})
    | ^(VAR_DECLARATION ^(VAR_DECLARATOR_LIST ^(VAR_DECLARATOR ^(ARRAY_ELEMENT_ACCESS a=primaryExpression b=expression))) ^(EXPR e=expr)) {
          LuaAST node = ((LuaAST)e.start);
          if( node.evalType == SymbolTable._void ){
            Symbol ts = currentScope.resolve(node.getText());
            if(ts != null){
		            node.type = ts.type;
		            node.evalType = ts.type;
		        }
          }
        } -> field5_tmpl(tname={$a.st}, expr1={$b.st}, expr2={$e.st}, type={$e.start.evalType})
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

    |   ^(CONCAT_STR a=expr b=expr)               ->  concat_tmpl(a={$a.st}, aType={$a.start.evalType}, b={$b.st},  bType={$b.start.evalType})
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
-> arrAccess_tmpl(name={$a.st}, index={$b.st}, ptype={$a.start.evalType});

methodCall
@init{
    String prefix = null;
    ArrayList rList = new ArrayList();
    ArrayList newstlist = new ArrayList();
}
//:^(METHOD_CALL a=methodQualifiedMember ^(ARGUMENT_LIST ((e+=expression)+)?)){
//(^(EXPR(e=expr{rList.add(e.st);eList.add(e.getStart());})+))*
:^(METHOD_CALL a=methodQualifiedMember ^(ARGUMENT_LIST ((e+=expression{rList.add(e.getTemplate());newstlist.add(e.getStart());})+)? )) {
//:^(METHOD_CALL a=methodQualifiedMember ^(ARGUMENT_LIST ((e+=expression){rList.add(e.getStart());})* )) {
      if($a.start.symbol != null){
          prefix = $a.start.symbol.def.declaredIn;
      }else{
          prefix = $a.start.declaredIn;
      }
    if(prefix != null && prefix.equals(fileName)){
        prefix = "self";
    }else{
      if(prefix != null)
        imports.add(prefix + ".h");
    }
    String name = ($a.st).toString();
    System.out.println($a.start.evalType + " in Method call of " + $a.st);
    if($a.start.evalType == null){
     // System.out.println($a.start.evalType + " in Method call of " + $a.st);
    }
//    if($a.start.evalType != null && $a.start.evalType.toString().equals("skinType")){
//      $a.start.type = $a.start.evalType;
//      $a.start.symbol.type = $a.start.evalType;
//      $a.start.symbol.def.type = $a.start.evalType;
//    }
    if($a.start.evalType != null){
      StringTemplate code12 = templateLib.getInstanceOf("checkAbstractTypeMap");
      code12.setAttribute("type",$a.start.evalType);
      if(code12.toString().length() > 1){
		      $a.start.type = $a.start.evalType;
		      if($a.start.symbol != null){
				      $a.start.symbol.type = $a.start.evalType;
				      $a.start.symbol.def.type = $a.start.evalType;
				  }
       }
    }

    StringTemplate tisDict = templateLib.getInstanceOf("checkDictionary");
    tisDict.setAttribute("method",$a.st);
    boolean isDictionary = false;
    boolean isArrMethod = false;
    boolean isWidget = false;
    
    if(tisDict.toString() != null && tisDict.toString().length() > 1){
        isDictionary = true;
    }     
    
    tisDict = templateLib.getInstanceOf("checkArrayMap");
    tisDict.setAttribute("method",$a.st);
    if(tisDict.toString() != null && tisDict.toString().length() > 1){
        isArrMethod = true;
    }     
    //arrayMethodMap
    tisDict = templateLib.getInstanceOf("checkWidgetMap");
    tisDict.setAttribute("method",$a.st);
    if(tisDict.toString() != null && tisDict.toString().length() > 1){
        isWidget = true;
    }     

    $st = templateLib.getInstanceOf("methodCall1_tmpl");
    $st.setAttribute("name", $a.st);
    $st.setAttribute("prefix", prefix);

    Map argNames = new HashMap();
    MethodSymbol ms = (MethodSymbol) (a!=null?((LuaAST)a.start):null).symbol;
    if(ms != null && !rList.isEmpty() && prefix != null && !isArrMethod && !isWidget && !isDictionary){
      System.err.println("METHODDDDDDDDDDDDD1 " + ms);
      //$st.setAttribute("prefix", prefix);
     
      java.util.Iterator itr = rList.iterator();
        Map<String, Symbol> orderedArgs =  ms.getMembers();
        java.util.Set<String> keys = orderedArgs.keySet();
        if(!rList.isEmpty()){
		        boolean rest = false;
		        for(String key : keys){
		          if(itr.hasNext()){
                  StringTemplate tmp = templateLib.getInstanceOf("callArgument_tmpl");
		              if(rest)
		                tmp.setAttribute("qual",key);
		              tmp.setAttribute("arg",itr.next());
		              $st.setAttribute("args", tmp);
		              rest = true;
		          }
		        }
		        if(!rest){
		        //Error condition. Argument count mismatch;
		          int count =0;
	            for(Object param : rList){
                  StringTemplate tmp = templateLib.getInstanceOf("callArgument_tmpl");
	               if(count > 0)
                    tmp.setAttribute("qual","param" + count);
                 tmp.setAttribute("arg",param);
                 $st.setAttribute("args", tmp);
                 count++;
	            }
		        }
		       // if(!rList.isEmpty())
		       //$st.setAttribute("args", tmp);
		    }
    }else{
      if(!isArrMethod && !isWidget && !isDictionary){
        $st.setAttribute("args", $e);
      }
    }
    if(isDictionary){
      StringTemplate tmp = templateLib.getInstanceOf("skinTemplate_tmpl");
      $st.setAttribute("args", $e);
      tmp.setAttribute("method",$st);
      $st = tmp;
    }
    if(isArrMethod){
      ArrayList listArgs = new ArrayList();
      StringTemplate tmp = templateLib.getInstanceOf("arrayMethod_tmpl");
      java.util.Iterator itr1 = rList.iterator();
      java.util.Iterator itr2 = newstlist.iterator();
      while(itr1.hasNext()){
        StringTemplate tmp1 = templateLib.getInstanceOf("argType_tmpl");
        Object tmpSt = itr1.next();
        tmp1.setAttribute("arg",tmpSt);
        LuaAST ast = (LuaAST) itr2.next();
        if(ms != null || ((LuaAST)ast.getChild(0)).symbol instanceof MethodSymbol){
          tmp1.setAttribute("type","Method");
          //Now add callback sign
        }else{
          tmp1.setAttribute("type",((LuaAST)ast.getChild(0)).evalType);
        }
        listArgs.add(tmp1);
      }
      //if(name.contains("Form") || name.contains("Label")){
        listArgs.add("@\"" + fileName + "\"");
      //}
      tmp.setAttribute("args", listArgs);
      $st.setAttribute("args",tmp);
    }
    if(isWidget){
      ArrayList listArgs = new ArrayList();
      StringTemplate tmp = templateLib.getInstanceOf("widgetMethod_tmpl");
      tmp.setAttribute("args", $e);
      $st.setAttribute("args",tmp);
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
          localFunctions.put(tfname, code);
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
        } -> field5_tmpl(tname={$tname}, expr1={$e1.st}, expr2={$e2.st},type={$e2.start.evalType})
      | ^(VAR_DECLARATOR IDENT e=expression)  {
        } -> field4_tmpl(tname={$tname}, fname={$IDENT.text}, expr={$e.st}, type={$e.start.evalType})
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
  : 'nil'   { $start.getToken().setText("nil"); }
  | 'true'   { $start.getToken().setText("true"); }       
  | 'false' { $start.getToken().setText("false"); }       
  | string  { $start.getToken().setText("@" + $start.getToken().getText());}
  | number
  | floatLit
  | ELLIPSIS 
;


number : INT_LITERAL | HEX_LITERAL;
floatLit: FLOAT_LITERAL | EXP_LITERAL;
string  : NORMALSTRING_LITERAL | CHARSTRING_LITERAL | LONGSTRING_LITERAL;
