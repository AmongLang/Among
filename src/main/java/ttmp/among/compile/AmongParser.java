package ttmp.among.compile;

import org.jetbrains.annotations.Nullable;
import ttmp.among.AmongEngine;
import ttmp.among.compile.Report.ReportType;
import ttmp.among.definition.AmongDefinition;
import ttmp.among.definition.MacroDefinition;
import ttmp.among.definition.MacroParameter;
import ttmp.among.definition.MacroParameterList;
import ttmp.among.definition.MacroType;
import ttmp.among.definition.OperatorDefinition;
import ttmp.among.definition.OperatorRegistry;
import ttmp.among.definition.OperatorType;
import ttmp.among.exception.Sussy;
import ttmp.among.obj.Among;
import ttmp.among.obj.AmongList;
import ttmp.among.obj.AmongObject;
import ttmp.among.obj.AmongPrimitive;
import ttmp.among.obj.AmongRoot;
import ttmp.among.util.RootAndDefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ttmp.among.compile.AmongToken.TokenType.*;

/**
 * Eats token. Shits object. Crazy.
 */
public final class AmongParser{
	private final AmongRoot root;
	/**
	 * Macros and operators defined or imported with {@code use public} statement. Will be returned as compilation
	 * result
	 */
	private final AmongDefinition definition;
	/**
	 * Imported macros and operators. Will be discarded along with parser.
	 */
	private final AmongDefinition importDefinition;
	private final AmongEngine engine;
	private final AmongTokenizer tokenizer;
	private final List<Report> reports = new ArrayList<>();

	private boolean recovering;
	@Nullable private MacroParameterList currentMacroParameter;

	public AmongParser(Source source, AmongEngine engine, AmongRoot root, AmongDefinition importDefinition){
		this.engine = engine;
		this.root = root;
		this.definition = new AmongDefinition();
		this.importDefinition = importDefinition;
		this.tokenizer = new AmongTokenizer(source, this);
	}

	public AmongEngine engine(){
		return engine;
	}
	public AmongDefinition importRoot(){
		return importDefinition;
	}

	public CompileResult parse(){
		try{
			among();
		}catch(Halt ignored){
			// no-op
		}catch(RuntimeException ex){
			reportError("Unexpected error", ex);
		}
		return new CompileResult(tokenizer.source(), root, definition, reports);
	}

	private void among(){
		while(true){
			tokenizer.discard();
			AmongToken next = tokenizer.next(true, TokenizationMode.PLAIN_WORD);
			if(next.is(EOF)) return;
			switch(next.keywordOrEmpty()){
				case "macro": macroDefinition(next.start); continue;
				case "operator": operatorDefinition(next.start, false); continue;
				case "keyword": operatorDefinition(next.start, true); continue;
				case "undef": switch(tokenizer.next(true, TokenizationMode.PLAIN_WORD).keywordOrEmpty()){
					case "macro": undefMacro(); continue;
					case "operator": undefOperation(false); continue;
					case "keyword": undefOperation(true); continue;
					default:
						reportError("Expected 'macro', 'operator' or 'keyword'");
						skipUntilLineBreak();
						continue;
				}
				case "use": use(); continue;
			}
			tokenizer.reset(next.isSimpleLiteral());
			Among a = nameable(false);
			if(a==null){
				next = tokenizer.next(true, TokenizationMode.WORD);
				if(!next.is(QUOTED_PRIMITIVE)){
					reportError("Top level statements can only be macro/operator/"+
							"keyword definition, undef statement, named/unnamed collections,"+
							" or primitives denoted with ' or \"");
					tryToRecover(TokenizationMode.WORD);
					continue;
				}
				a = Among.value(next.expectLiteral());
			}
			root.addObject(a);
		}
	}

	@Nullable private String definitionName(TokenizationMode mode){
		tokenizer.discard();
		AmongToken next = tokenizer.next(true, mode);
		if(!next.isLiteral()){
			reportError("Expected name");
			tokenizer.reset();
			tryToRecover(mode);
			return null;
		}
		return next.expectLiteral();
	}

	private void macroDefinition(int startIndex){
		String name = definitionName(TokenizationMode.MACRO_NAME);
		if(name==null) return;
		tokenizer.discard();
		switch(tokenizer.next(true, TokenizationMode.PLAIN_WORD).type){
			case COLON: macroDefinition(startIndex, name, MacroType.CONST); break;
			case L_BRACE: macroDefinition(startIndex, name, MacroType.OBJECT); break;
			case L_BRACKET: macroDefinition(startIndex, name, MacroType.LIST); break;
			case L_PAREN: macroDefinition(startIndex, name, MacroType.OPERATION); break;
			default:
				reportError("Invalid macro statement; expected '{', '[', '(' or ':'");
				tokenizer.reset();
				tryToRecover(TokenizationMode.PLAIN_WORD);
		}
	}
	private void macroDefinition(int startIndex, String name, MacroType type){
		MacroParameterList params;
		if(type==MacroType.CONST) params = MacroParameterList.of();
		else{
			switch(type){
				case OBJECT: params = macroParam(R_BRACE); break;
				case LIST: params = macroParam(R_BRACKET); break;
				case OPERATION: params = macroParam(R_PAREN); break;
				default: throw new IllegalStateException("Unreachable");
			}
			tokenizer.discard();
			if(!tokenizer.next(true, TokenizationMode.UNEXPECTED).is(COLON)){
				reportError("Expected ':' after parameter definition");
				tokenizer.reset();
				tryToRecover(TokenizationMode.WORD);
				return;
			}
		}
		if(params!=null){
			if(this.currentMacroParameter!=null)
				reportError("Previous macro parameter is not cleaned up properly, this shouldn't happen");
			this.currentMacroParameter = params;
		}
		Among expr = exprOrError();
		this.currentMacroParameter = null;
		tokenizer.discard();
		AmongToken next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
		if(!next.is(BR)&&!next.is(EOF)){
			reportError("Expected newline after macro statement");
			tokenizer.reset();
			tryToRecover(TokenizationMode.PLAIN_WORD);
		}
		if(params!=null){
			if((type==MacroType.LIST||type==MacroType.OPERATION)&&!params.hasConsecutiveOptionalParams()){
				reportError("Optional parameters of "+(type==MacroType.LIST ? "list" : "operation")+
						" macro should be consecutive, placed at end of the parameter list", startIndex);
				return;
			}
			MacroDefinition macro = new MacroDefinition(name, type, params, expr);
			definition.addMacro(macro);
			importDefinition.addMacro(macro);
		}
	}

	@Nullable private MacroParameterList macroParam(AmongToken.TokenType closure){
		List<MacroParameter> params = new ArrayList<>();
		Set<String> nameSet = new HashSet<>();
		boolean invalid = false;
		while(true){
			AmongToken next = tokenizer.next(true, TokenizationMode.PARAM_NAME);
			if(next.is(closure)) break;
			if(next.is(EOF)) break; // It will be reported in defMacro()
			if(!next.is(PARAM_NAME)){
				reportError("Expected parameter name");
				invalid = true;
				if(tryToRecover(TokenizationMode.PARAM_NAME, closure, true)) break;
				else continue;
			}
			String name = next.expectLiteral();
			if(!nameSet.add(name)){
				reportError("Duplicated parameter '"+name+"'");
				invalid = true;
			}
			next = tokenizer.next(true, TokenizationMode.PARAM_NAME);

			Among defaultValue;
			if(next.is(EQ)){
				defaultValue = exprOrError();
				next = tokenizer.next(true, TokenizationMode.PARAM_NAME);
			}else defaultValue = null;
			if(!invalid) params.add(new MacroParameter(name, defaultValue));

			if(next.is(closure)) break;
			else if(!next.is(COMMA)){
				reportError("Expected ',' or "+closure.friendlyName());
				invalid = true;
				if(tryToRecover(TokenizationMode.PARAM_NAME, closure, true)) break;
			}
		}
		return invalid ? null : MacroParameterList.of(params);
	}

	private void operatorDefinition(int startIndex, boolean keyword){
		String name = definitionName(TokenizationMode.PLAIN_WORD);
		if(name==null) return;
		OperatorType type;
		if(!tokenizer.next(true, TokenizationMode.PLAIN_WORD).is(PLAIN_WORD, "as")){
			reportError("Expected 'as'");
			skipUntilLineBreak();
			return;
		}
		switch(tokenizer.next(true, TokenizationMode.PLAIN_WORD).keywordOrEmpty()){
			case "binary": type = OperatorType.BINARY; break;
			case "prefix": type = OperatorType.PREFIX; break;
			case "postfix": type = OperatorType.POSTFIX; break;
			default:
				reportError("Expected keyword; 'binary', 'prefix' or 'postfix'");
				skipUntilLineBreak();
				return;
		}
		tokenizer.discard();
		double priority = Double.NaN;
		AmongToken next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
		if(next.is(COLON)){
			priority = tokenizer.next(true, TokenizationMode.VALUE)
					.asNumber();
			if(Double.isNaN(priority)) reportError("Expected number");
		}else tokenizer.reset(next.is(ERROR));
		next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
		if(!next.is(BR)&&!next.is(EOF)){
			reportError("Expected newline after "+(keyword ? "keyword" : "operator")+" statement");
			skipUntilLineBreak();
			return;
		}
		OperatorDefinition operator = new OperatorDefinition(name, keyword, type, priority);
		OperatorRegistry.RegistrationResult result = importDefinition.operators().add(operator);
		if(!result.isSuccess())
			report(engine.allowInvalidOperatorRegistration ?
							ReportType.WARN : ReportType.ERROR,
					result.message(operator), startIndex);
		definition.operators().add(operator);
	}

	private void undefMacro(){
		String name = definitionName(TokenizationMode.WORD);
		if(name==null) return;
		AmongToken next = tokenizer.next(false, TokenizationMode.PLAIN_WORD);
		switch(next.type){
			case BR:
				definition.removeMacro(name, MacroType.CONST);
				importDefinition.removeMacro(name, MacroType.CONST);
				return;
			case L_BRACE:
				expectNext(R_BRACE);
				definition.removeMacro(name, MacroType.OBJECT);
				importDefinition.removeMacro(name, MacroType.OBJECT);
				break;
			case L_BRACKET:
				expectNext(R_BRACKET);
				definition.removeMacro(name, MacroType.LIST);
				importDefinition.removeMacro(name, MacroType.LIST);
				break;
			case L_PAREN:
				expectNext(R_PAREN);
				definition.removeMacro(name, MacroType.OPERATION);
				importDefinition.removeMacro(name, MacroType.OPERATION);
				break;
			default:
				reportError("Expected '{', '[', '(' or line break");
				skipUntilLineBreak();
				return;
		}
		next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
		if(!next.is(BR)&&!next.is(EOF)){
			reportError("Expected newline after undef statement");
			skipUntilLineBreak();
		}
	}

	private void undefOperation(boolean keyword){
		String name = definitionName(TokenizationMode.WORD);
		if(name==null) return;
		definition.operators().remove(name, keyword);
		importDefinition.operators().remove(name, keyword);
		AmongToken next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
		if(!next.is(BR)&&!next.is(EOF)){
			reportError("Expected newline after undef statement");
			skipUntilLineBreak();
		}
	}

	private void use(){
		tokenizer.discard();
		AmongToken next = tokenizer.next(false, TokenizationMode.PLAIN_WORD);
		boolean pub = next.keywordOrEmpty().equals("public");
		if(!pub) tokenizer.reset(true);
		next = tokenizer.next(false, TokenizationMode.VALUE);
		if(!next.isLiteral()){
			reportError("Expected path");
			skipUntilLineBreak();
			return;
		}
		String path = next.expectLiteral();
		RootAndDefinition imported = engine.getOrReadFrom(path);
		if(imported==null){
			reportError("Invalid use statement: Cannot resolve definitions from path '"+path+"'");
		}else{
			copyDefinitions(imported.definition(), importDefinition);
			if(pub) copyDefinitions(imported.definition(), definition);
		}
		next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
		if(!next.is(BR)&&!next.is(EOF)){
			reportError("Expected newline after undef statement");
			skipUntilLineBreak();
		}
	}

	private void copyDefinitions(AmongDefinition from, AmongDefinition to){
		for(MacroDefinition macro : from.macros().values()) to.addMacro(macro);
		from.operators().forEachOperatorAndKeyword(to.operators()::add); // TODO log failure
	}

	private void expectNext(AmongToken.TokenType type){
		if(!tokenizer.next(true, TokenizationMode.UNEXPECTED).is(type)){
			reportError("Expected "+type);
			tryToRecover(TokenizationMode.UNEXPECTED, type, false, false);
		}
	}

	private Among exprOrError(){
		Among among = expr();
		return among==null ? Among.value("ERROR") : among;
	}
	@Nullable private Among expr(){
		tokenizer.discard();
		Among a = nameable(false);
		if(a!=null) return a;
		tokenizer.reset(true);
		AmongToken next = tokenizer.next(true, TokenizationMode.VALUE);
		if(!next.isLiteral()){
			reportError("Expected value");
			tokenizer.reset(true);
			return null;
		}
		AmongPrimitive p = Among.value(next.expectLiteral());
		if(next.is(QUOTED_PRIMITIVE)) return p;
		if(isParamRef(next)) p.setParamRef(true);
		return primitiveMacro(p, next.start);
	}

	private boolean isParamRef(AmongToken token){
		return currentMacroParameter!=null&&
				token.isLiteral()&&
				!token.is(QUOTED_PRIMITIVE)&&
				currentMacroParameter.parameters().containsKey(token.expectLiteral());
	}

	@Nullable private Among nameable(boolean operation){
		tokenizer.discard();
		AmongToken next = tokenizer.next(true, operation ? TokenizationMode.OPERATION : TokenizationMode.VALUE);
		Among nameable;
		switch(next.type){
			case L_BRACE: return obj(null);
			case L_BRACKET: return list(null);
			case L_PAREN:{
				AmongList o = oper(null);
				return engine.collapseUnaryOperation&&!o.hasName()&&o.size()==1 ? o.get(0) : o;
			}
			default:
				if(next.isLiteral()){
					boolean paramRef = isParamRef(next), applyMacro = !next.is(QUOTED_PRIMITIVE);
					// lookahead to find if it's nameable instance
					switch(tokenizer.next(operation, TokenizationMode.UNEXPECTED).type){
						case L_BRACE:{
							AmongObject o = obj(next.expectLiteral());
							if(applyMacro&&!paramRef) return objectMacro(o, next.start);
							else nameable = o;
							break;
						}
						case L_BRACKET:{
							AmongList l = list(next.expectLiteral());
							if(applyMacro&&!paramRef) return listMacro(l, next.start);
							else nameable = l;
							break;
						}
						case L_PAREN:{
							AmongList o = oper(next.expectLiteral());
							if(applyMacro&&!paramRef) return operationMacro(o, next.start);
							else nameable = engine.collapseUnaryOperation&&!o.hasName()&&o.size()==1 ? o.get(0) : o;
							break;
						}
						default: tokenizer.reset(true); return null;
					}
					nameable.setParamRef(paramRef);
					return nameable;
				}
				tokenizer.reset();
				return null;
		}
	}

	private AmongObject obj(@Nullable String name){
		AmongObject object = Among.namedObject(name);
		L:
		while(true){
			AmongToken keyToken = tokenizer.next(true, TokenizationMode.KEY);
			switch(keyToken.type){
				case EOF: reportError("Unterminated object");
				case R_BRACE: break L;
			}
			if(!keyToken.isLiteral()){
				reportError("Expected property key");
				if(tryToRecover(TokenizationMode.KEY, R_BRACE, true)) break;
				else continue;
			}

			if(!tokenizer.next(true, TokenizationMode.UNEXPECTED).is(COLON)){
				reportError("Expected ':' after property key");
				if(tryToRecover(TokenizationMode.KEY, R_BRACE, true)) break;
				else continue;
			}
			String key = keyToken.expectLiteral();
			if(object.hasProperty(key))
				report(engine.allowDuplicateObjectProperty ? ReportType.WARN : ReportType.ERROR,
						"Property '"+key+"' is already defined", keyToken.start);

			Among expr = exprOrError();

			if(!object.hasProperty(key)) object.setProperty(key, expr);
			AmongToken next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
			switch(next.type){
				case BR:
					tokenizer.discard();
					next = tokenizer.next(true, TokenizationMode.KEY);
					if(!next.is(AmongToken.TokenType.COMMA)) tokenizer.reset();
					break;
				case COMMA: break;
				case EOF: reportError("Unterminated object");
				case R_BRACE: break L;
				default:
					reportError("Each object property should be separated with either line breaks or ','");
					if(tryToRecover(TokenizationMode.KEY, R_BRACE, true)) break;
			}
		}
		return object;
	}

	private AmongList list(@Nullable String name){
		AmongList list = Among.namedList(name);
		L:
		while(true){
			tokenizer.discard();
			AmongToken next = tokenizer.next(true, TokenizationMode.UNEXPECTED);
			switch(next.type){
				case EOF: reportError("Unterminated list");
				case R_BRACKET: break L;
			}
			tokenizer.reset(next.is(ERROR));
			Among expr = expr();
			if(expr!=null) list.add(expr);
			next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
			switch(next.type){
				case BR:
					tokenizer.discard();
					next = tokenizer.next(true, TokenizationMode.UNEXPECTED);
					if(!next.is(AmongToken.TokenType.COMMA))
						tokenizer.reset(next.is(ERROR));
					break;
				case COMMA: break;
				case EOF: reportError("Unterminated list");
				case R_BRACKET: break L;
				default:
					reportError("Each value should be separated with either line breaks or ','");
					if(tryToRecover(TokenizationMode.VALUE, R_BRACKET, true)) break;
			}
		}
		return list;
	}

	private AmongList oper(@Nullable String name){
		AmongList list = Among.namedList(name);
		L:
		while(true){
			tokenizer.discard();
			switch(tokenizer.next(true, TokenizationMode.OPERATION).type){
				case EOF: reportError("Unterminated operation");
				case R_PAREN: break L;
			}
			tokenizer.reset();
			list.add(operationExpression(importDefinition.operators().priorityGroup(), 0));
			tokenizer.discard();
			switch(tokenizer.next(true, TokenizationMode.OPERATION).type){
				case COMMA: continue;
				case EOF: reportError("Unterminated operation");
				case R_PAREN: break L;
				default:
					reportError("Each term should be separated with ','");
					tokenizer.reset();
			}
		}
		return list;
	}

	private Among operationExpression(List<OperatorRegistry.PriorityGroup> operators, int i){
		if(i<operators.size()){ // check for operators and keywords
			OperatorRegistry.PriorityGroup group = operators.get(i);
			switch(group.type()){
				case BINARY:{
					Among a = operationExpression(operators, i+1);
					while(true){
						tokenizer.discard();
						AmongToken next = tokenizer.next(true, TokenizationMode.OPERATION);
						if(next.isOperatorOrKeyword()){
							OperatorDefinition op = group.get(next.expectLiteral());
							if(op!=null){
								a = operationMacro(Among.namedList(op.name(), a, operationExpression(operators, i+1)), next.start);
								continue;
							}
						}
						tokenizer.reset();
						return a;
					}
				}
				case POSTFIX:{
					Among a = operationExpression(operators, i+1);
					while(true){
						tokenizer.discard();
						AmongToken next = tokenizer.next(true, TokenizationMode.OPERATION);
						if(next.isOperatorOrKeyword()){
							OperatorDefinition op = group.get(next.expectLiteral());
							if(op!=null){
								a = operationMacro(Among.namedList(op.name(), a), next.start);
								continue;
							}
						}
						tokenizer.reset();
						return a;
					}
				}
				case PREFIX: return prefix(operators, i);
				default: throw new IllegalStateException("Unreachable");
			}
		}
		// read primitive
		tokenizer.discard();
		Among a = nameable(true);
		if(a!=null) return a;
		tokenizer.reset();
		AmongToken next = tokenizer.next(true, TokenizationMode.OPERATION);
		if(!next.isLiteral()){
			reportError("Expected value");
			tokenizer.reset();
			return Among.value("ERROR");
		}
		AmongPrimitive p = Among.value(next.expectLiteral());
		if(isParamRef(next)) p.setParamRef(true);
		return next.is(QUOTED_PRIMITIVE) ? p : primitiveMacro(p, next.start);
	}

	private Among prefix(List<OperatorRegistry.PriorityGroup> operators, int i){
		tokenizer.discard();
		AmongToken next = tokenizer.next(true, TokenizationMode.OPERATION);
		if(next.isOperatorOrKeyword()){
			OperatorDefinition op = operators.get(i).get(next.expectLiteral());
			if(op!=null) return operationMacro(Among.namedList(op.name(), prefix(operators, i)), next.start);
		}
		tokenizer.reset();
		return operationExpression(operators, i+1);
	}

	private Among primitiveMacro(AmongPrimitive primitive, int sourcePosition){
		return macro(primitive, primitive.getValue(), MacroType.CONST, sourcePosition);
	}
	private Among objectMacro(AmongObject object, int sourcePosition){
		return macro(object, object.getName(), MacroType.OBJECT, sourcePosition);
	}
	private Among listMacro(AmongList list, int sourcePosition){
		return macro(list, list.getName(), MacroType.LIST, sourcePosition);
	}
	private Among operationMacro(AmongList operation, int sourcePosition){
		return macro(operation, operation.getName(), MacroType.OPERATION, sourcePosition);
	}
	private Among macro(Among target, String macroName, MacroType macroType, int sourcePosition){
		MacroDefinition macro = importDefinition.searchMacro(macroName, macroType);
		if(macro==null) return target;
		try{
			return macro.apply(target, engine.copyMacroConstant,
					(t, s) -> report(t, s, sourcePosition));
		}catch(Sussy sussy){
			return Among.value("ERROR");
		}
	}

	void reportWarning(String message, String... hints){
		report(ReportType.WARN, message, hints);
	}
	void reportWarning(String message, int srcIndex, String... hints){
		report(ReportType.WARN, message, srcIndex, hints);
	}
	void reportWarning(String message, @Nullable Throwable ex, String... hints){
		report(ReportType.WARN, message, ex, hints);
	}
	void reportError(String message, String... hints){
		report(ReportType.ERROR, message, hints);
	}
	void reportError(String message, int srcIndex, String... hints){
		report(ReportType.ERROR, message, srcIndex, hints);
	}
	void reportError(String message, @Nullable Throwable ex, String... hints){
		report(ReportType.ERROR, message, ex, hints);
	}

	void report(ReportType type, String message, String... hints){
		report(type, message, null, hints);
	}
	void report(ReportType type, String message, int srcIndex, String... hints){
		report(type, message, srcIndex, null, hints);
	}

	void report(ReportType type, String message, @Nullable Throwable ex, String... hints){
		AmongToken lastToken = tokenizer.lastToken();
		report(type, message, lastToken!=null ? lastToken.start : -1, ex, hints);
	}
	void report(ReportType type, String message, int srcIndex, @Nullable Throwable ex, String... hints){
		if(!recovering) reports.add(new Report(type, message, srcIndex, ex, hints));
	}

	private void skipUntilLineBreak(){
		while(true){
			switch(tokenizer.next(false, TokenizationMode.WORD).type){
				case BR: case EOF: return;
			}
		}
	}

	/**
	 * Attempt to continue the compilation process by throwing away some tokens. It will still try to account for
	 * object/list/whatever definitions and parse them recursively before throwing it away again.
	 *
	 * @param mode Tokenization mode to use for
	 */
	private void tryToRecover(TokenizationMode mode){
		tryToRecover(mode, null, false);
	}
	/**
	 * Attempt to continue the compilation process by throwing away some tokens. It will still try to account for
	 * object/list/whatever definitions and parse them recursively before throwing it away again.
	 *
	 * @param mode          Tokenization mode to use for
	 * @param closure       Closure token to search for
	 * @param returnOnComma This method return on comma if the value is {@code true}
	 * @return Whether it found the closure or not
	 */
	private boolean tryToRecover(TokenizationMode mode, @Nullable AmongToken.TokenType closure, boolean returnOnComma){
		return tryToRecover(mode, closure, returnOnComma, true);
	}
	/**
	 * Attempt to continue the compilation process by throwing away some tokens. It will still try to account for
	 * object/list/whatever definitions and parse them recursively before throwing it away again.
	 *
	 * @param mode              Tokenization mode to use for
	 * @param closure           Closure token to search for
	 * @param returnOnComma     Returns on comma if the value is {@code true}
	 * @param returnOnLineBreak Returns on line break if the value is {@code true}
	 * @return Whether it found the closure or not
	 */
	private boolean tryToRecover(TokenizationMode mode, @Nullable AmongToken.TokenType closure, boolean returnOnComma, boolean returnOnLineBreak){
		boolean prevRecovering = this.recovering;
		this.recovering = true;
		while(true){
			tokenizer.discard();
			AmongToken.TokenType t = tokenizer.next(false, mode).type;
			switch(t){
				case BR: if(!returnOnLineBreak) continue;
				case EOF: this.recovering = prevRecovering; return false; // continue from here (well, there might not be much to do if it's EOF lmao)
				case COMMA:
					if(!returnOnComma) continue;
					this.recovering = prevRecovering;
					return false;
				case L_BRACE: case L_BRACKET: case L_PAREN:
					tokenizer.reset();
					nameable(false); // read object and throw it away
					continue;
				default: if(t==closure){
					this.recovering = prevRecovering;
					return true;
				}
			}
		}
	}

	/**
	 * Special exception for halting the process
	 */
	private static final class Halt extends Sussy{}
}
