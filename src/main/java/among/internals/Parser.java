package among.internals;

import among.AmongDefinition;
import among.AmongEngine;
import among.AmongRoot;
import among.AmongWalker;
import among.CompileResult;
import among.NodePath;
import among.Report;
import among.ReportType;
import among.RootAndDefinition;
import among.Source;
import among.TypeFlags;
import among.macro.Macro;
import among.macro.MacroDefinition;
import among.macro.MacroParameter;
import among.macro.MacroParameterList;
import among.macro.MacroRegistry;
import among.macro.MacroReplacement;
import among.macro.MacroReplacement.MacroOp;
import among.macro.MacroReplacement.MacroOp.MacroCall;
import among.macro.MacroReplacement.MacroOp.NameReplacement;
import among.macro.MacroReplacement.MacroOp.ValueReplacement;
import among.macro.MacroSignature;
import among.macro.MacroType;
import among.obj.Among;
import among.obj.AmongList;
import among.obj.AmongNameable;
import among.obj.AmongObject;
import among.obj.AmongPrimitive;
import among.operator.OperatorDefinition;
import among.operator.OperatorProperty;
import among.operator.OperatorRegistry;
import among.operator.OperatorType;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static among.internals.Token.TokenType.*;

/**
 * Eats token. Shits object. Crazy.
 */
public final class Parser{
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
	private final Tokenizer tokenizer;
	private final List<Report> reports = new ArrayList<>();

	private boolean recovering;
	@Nullable private ParsingMacro currentMacro;

	public Parser(Source source, AmongEngine engine, AmongRoot root, AmongDefinition importDefinition){
		this.engine = engine;
		this.root = root;
		this.definition = new AmongDefinition();
		this.importDefinition = importDefinition;
		this.tokenizer = new Tokenizer(source, this);
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
		}catch(RuntimeException ex){
			reportError("Unexpected error", ex);
		}
		return new CompileResult(tokenizer.source(), root, definition, reports);
	}

	private void among(){
		while(true){
			tokenizer.discard();
			Token next = tokenizer.next(true, TokenizationMode.PLAIN_WORD);
			if(next.is(EOF)) return;
			switch(next.keywordOrEmpty()){
				case "macro": macroDefinition(false, next.start); continue;
				case "fn": macroDefinition(true, next.start); continue;
				case "operator": operatorDefinition(next.start, false); continue;
				case "keyword": operatorDefinition(next.start, true); continue;
				case "undef":
					switch(tokenizer.next(true, TokenizationMode.PLAIN_WORD).keywordOrEmpty()){
						case "macro": undefMacro(false); break;
						case "fn": undefMacro(true); break;
						case "operator": undefOperation(false); break;
						case "keyword": undefOperation(true); break;
						case "use": undefUse(); break;
						default:
							reportError("Expected 'macro', 'operator' or 'keyword'");
							tryToRecover(TokenizationMode.UNEXPECTED, null, true, true);
							continue;
					}
					expectStmtEnd("Expected ',' or newline after undef statement");
					continue;
				case "use": use(next.start); continue;
				default:
					tokenizer.reset(next.isSimpleLiteral());
					Among a = nameable(false);
					if(a==null){
						next = tokenizer.next(true, TokenizationMode.VALUE);
						if(!next.isLiteral()){
							if(next.is(COMMA))
								reportError("Redundant comma");
							else{
								reportError("Top level statements can only be macro/operator/"+
										"keyword definition, undef statement, or values.");
								tryToRecover(TokenizationMode.UNEXPECTED, null, true, true);
							}
							continue;
						}
						a = Among.value(next.expectLiteral());
					}
					root.add(a);
					stmtEnd();
			}
		}
	}

	/**
	 * Checks if there's appropriate statement end; if not, tokens are discarded until a statement end is found.
	 *
	 * @param errorMessage Error message to report if statement end is missing
	 * @return Whether there is appropriate statement end
	 */
	private boolean expectStmtEnd(String errorMessage){
		if(stmtEnd()) return true;
		reportError(errorMessage);
		tryToRecover(TokenizationMode.UNEXPECTED, null, true, true);
		return false;
	}

	/**
	 * Checks if there's appropriate statement end.
	 *
	 * @return Whether there is appropriate statement end
	 */
	private boolean stmtEnd(){
		tokenizer.discard();
		Token next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
		switch(next.type){
			case BR:
				tokenizer.discard();
				next = tokenizer.next(true, TokenizationMode.UNEXPECTED);
				if(!next.is(COMMA)) tokenizer.reset(next.is(ERROR));
			case EOF: case COMMA: return true;
			default: tokenizer.reset(next.is(ERROR)); return false;
		}
	}

	@Nullable private String definitionName(TokenizationMode mode){
		tokenizer.discard();
		Token next = tokenizer.next(true, mode);
		if(!next.isLiteral()){
			reportError("Expected name");
			tokenizer.reset();
			tryToRecover(mode);
			return null;
		}
		return next.expectLiteral();
	}

	private void macroDefinition(boolean fn, int startIndex){
		String name = definitionName(TokenizationMode.MACRO_NAME);
		if(name==null) return;
		tokenizer.discard();
		switch(tokenizer.next(true, TokenizationMode.PLAIN_WORD).type){
			case COLON: macroDefinition(startIndex, name, fn ? MacroType.ACCESS : MacroType.CONST); break;
			case L_BRACE: macroDefinition(startIndex, name, fn ? MacroType.OBJECT_FN : MacroType.OBJECT); break;
			case L_BRACKET: macroDefinition(startIndex, name, fn ? MacroType.LIST_FN : MacroType.LIST); break;
			case L_PAREN: macroDefinition(startIndex, name, fn ? MacroType.OPERATION_FN : MacroType.OPERATION); break;
			default:
				reportError("Invalid macro statement; expected '{', '[', '(' or ':'");
				tokenizer.reset();
				tryToRecover(TokenizationMode.PLAIN_WORD);
		}
	}
	private void macroDefinition(int startIndex, String name, MacroType type){
		ParsingMacro m = new ParsingMacro(startIndex, name, type);
		if(type!=MacroType.CONST&&type!=MacroType.ACCESS){
			switch(type){
				case OBJECT: case OBJECT_FN: macroParam(m, R_BRACE); break;
				case LIST: case LIST_FN: macroParam(m, R_BRACKET); break;
				case OPERATION: case OPERATION_FN: macroParam(m, R_PAREN); break;
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

		if(this.currentMacro!=null)
			reportError("Previous macro parameter is not cleaned up properly, this shouldn't happen");
		this.currentMacro = m;
		Among expr = exprOrError();
		tokenizer.discard();
		expectStmtEnd("Expected ',' or newline after macro statement");
		m.register(expr);
		this.currentMacro = null;
	}

	private void macroParam(ParsingMacro macro, Token.TokenType closure){
		while(true){
			Token next = tokenizer.next(true, TokenizationMode.PARAM_NAME);
			if(next.is(closure)) break;
			if(next.is(EOF)) break; // It will be reported in defMacro()
			if(!next.is(PARAM_NAME)){
				reportError("Expected parameter name");
				macro.invalid = true;
				if(tryToRecover(TokenizationMode.PARAM_NAME, closure, true)) break;
				else continue;
			}
			String name = next.expectLiteral();
			int nameStart = next.start;
			next = tokenizer.next(true, TokenizationMode.PARAM_NAME);

			Among defaultValue;
			if(next.is(EQ)){
				defaultValue = exprOrError();
				next = tokenizer.next(true, TokenizationMode.PARAM_NAME);
			}else defaultValue = null;
			macro.newParam(name, defaultValue, nameStart);

			if(next.is(closure)) break;
			else if(!next.is(COMMA)){
				reportError("Expected ',' or "+closure.friendlyName());
				macro.invalid = true;
				if(tryToRecover(TokenizationMode.PARAM_NAME, closure, true)) break;
			}
		}
	}

	private void operatorDefinition(int startIndex, boolean keyword){
		String name = definitionName(TokenizationMode.WORD);
		if(name==null) return;
		if(!tokenizer.next(true, TokenizationMode.PLAIN_WORD).is(PLAIN_WORD, "as")){
			reportError("Expected 'as'");
			tryToRecover(TokenizationMode.UNEXPECTED, null, true, true);
			return;
		}
		List<TypeAndProperty> list = new ArrayList<>();
		boolean invalid = false;
		while(true){
			TypeAndProperty p = operatorProperty();
			if(p!=null) list.add(p);
			else invalid = true;
			tokenizer.discard();
			Token next = tokenizer.next(true, TokenizationMode.PLAIN_WORD);
			if(next.is(PLAIN_WORD, "and")) continue;
			String alias;
			if(next.is(COLON)){
				next = tokenizer.next(true, TokenizationMode.VALUE);
				if(!next.isLiteral()){
					reportError("Expected literal");
					tryToRecover(TokenizationMode.UNEXPECTED, null, true, true);
					return;
				}else alias = next.expectLiteral();
			}else{
				alias = null;
				tokenizer.reset(next.isLiteral());
			}
			if(!invalid){
				for(TypeAndProperty e : list){
					OperatorDefinition operator = new OperatorDefinition(name, keyword, e.type, alias, e.properties, e.priority);
					OperatorRegistry.RegistrationResult result = importDefinition.operators().add(operator);
					if(!result.isSuccess())
						report(engine.allowInvalidOperatorRegistration ?
										ReportType.WARN : ReportType.ERROR,
								result.message(operator), startIndex);
					else definition.operators().add(operator);
				}
			}
			expectStmtEnd("Expected ',' or newline after undef statement");
			return;
		}
	}

	@Nullable private TypeAndProperty operatorProperty(){
		OperatorType type = null;
		EnumSet<OperatorPropertyEnum> properties = EnumSet.noneOf(OperatorPropertyEnum.class);
		boolean invalid = false;
		double priority = Double.NaN;
		L:
		while(true){
			tokenizer.discard();
			OperatorType t = null;
			OperatorPropertyEnum e = null;
			Token next = tokenizer.next(false, TokenizationMode.PLAIN_WORD);
			switch(next.keywordOrEmpty()){
				case "binary": t = OperatorType.BINARY; break;
				case "prefix": t = OperatorType.PREFIX; break;
				case "postfix": t = OperatorType.POSTFIX; break;
				case "left-associative": e = OperatorPropertyEnum.LEFT_ASSOCIATIVE; break;
				case "right-associative": e = OperatorPropertyEnum.RIGHT_ASSOCIATIVE; break;
				case "accessor": e = OperatorPropertyEnum.ACCESSOR; break;
				case "and": tokenizer.reset(); break L;
				case "":
					switch(next.type){
						case L_PAREN:
							tokenizer.discard();
							priority = tokenizer.next(true, TokenizationMode.VALUE).asNumber();
							if(Double.isNaN(priority)){
								reportError("Number expected");
								tokenizer.reset();
								tryToRecover(TokenizationMode.UNEXPECTED, R_PAREN, false, false);
								invalid = true;
							}else expectNext(R_PAREN);
							break L;
						case COLON: case BR: case COMMA: tokenizer.reset(); break L;
						case EOF: break L;
						default:
							reportError("Expected keyword");

							tryToRecover(TokenizationMode.UNEXPECTED, null, true, true);
							break L;
					}
				default:
					reportError("Unknown operator property; 'binary', 'prefix', 'postfix', 'left-associative', 'right-associative' or 'accessor' expected");
					continue;
			}
			if(t!=null){
				if(type!=null){
					reportError("Operator type defined twice: '"+type+"' and '"+t+"'");
					invalid = true;
				}else type = t;
			}else{
				if(!properties.add(e)){
					reportError("'"+e+"' defined twice");
					invalid = true;
				}
			}
		}
		if(invalid) return null;
		if(type==null) reportError("Missing operator type; needs either 'binary', 'prefix' or 'postfix'");
		else if(!properties.isEmpty()&&type!=OperatorType.BINARY)
			reportError("Prefix and postfix operators cannot have additional properties");
		else if(properties.contains(OperatorPropertyEnum.LEFT_ASSOCIATIVE)&&properties.contains(OperatorPropertyEnum.RIGHT_ASSOCIATIVE))
			reportError("'left-associative' and 'right-associative' at the same place");
		else if(properties.contains(OperatorPropertyEnum.RIGHT_ASSOCIATIVE)&&properties.contains(OperatorPropertyEnum.ACCESSOR))
			reportError("Cannot be 'right-associative' and 'accessor' at the same time");
		else{
			byte flags;
			if(properties.contains(OperatorPropertyEnum.RIGHT_ASSOCIATIVE)) flags = OperatorProperty.RIGHT_ASSOCIATIVE;
			else if(properties.contains(OperatorPropertyEnum.ACCESSOR)) flags = OperatorProperty.ACCESSOR;
			else flags = 0;
			return new TypeAndProperty(type, flags, priority);
		}
		return null;
	}

	private void undefMacro(boolean fn){
		String name = definitionName(TokenizationMode.MACRO_NAME);
		if(name==null) return;
		tokenizer.discard();
		Token next = tokenizer.next(false, TokenizationMode.PLAIN_WORD);
		MacroType type;
		switch(next.type){
			case BR: case EOF: case COMMA: tokenizer.reset(); type = fn ? MacroType.ACCESS : MacroType.CONST; break;
			case L_BRACE: expectNext(R_BRACE); type = fn ? MacroType.OBJECT_FN : MacroType.OBJECT; break;
			case L_BRACKET: expectNext(R_BRACKET); type = fn ? MacroType.LIST_FN : MacroType.LIST; break;
			case L_PAREN: expectNext(R_PAREN); type = fn ? MacroType.OPERATION_FN : MacroType.OPERATION; break;
			default:
				reportError("Expected '{', '[', '(' or end of statement");
				skipUntilLineBreak();
				return;
		}
		definition.macros().remove(name, type);
		importDefinition.macros().remove(name, type);
	}

	private void undefOperation(boolean keyword){
		String name = definitionName(TokenizationMode.WORD);
		if(name==null) return;
		definition.operators().remove(name, keyword);
		importDefinition.operators().remove(name, keyword);
	}

	private void undefUse(){
		Token next = tokenizer.next(false, TokenizationMode.VALUE);
		if(!next.isLiteral()){
			reportError("Expected path");
			tryToRecover(TokenizationMode.UNEXPECTED, null, true, true);
			return;
		}
		String path = next.expectLiteral();
		RootAndDefinition imported = engine.getOrReadFrom(path);
		if(imported==null){
			reportError("Invalid use statement: Cannot resolve definitions from path '"+path+"'");
		}else{
			imported.definition().macros().allMacroSignatures().forEach(s -> {
				importDefinition.macros().remove(s);
				definition.macros().remove(s);
			});
			imported.definition().operators().allOperatorNames().forEach(g -> {
				importDefinition.operators().remove(g.name(), g.isKeyword());
				definition.operators().remove(g.name(), g.isKeyword());
			});
		}
	}

	private void use(int startIndex){
		tokenizer.discard();
		Token next = tokenizer.next(false, TokenizationMode.PLAIN_WORD);
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
			reportError("Invalid use statement: Cannot resolve definitions from path '"+path+"'", startIndex);
		}else{
			copyDefinitions(imported.definition(), importDefinition, true, startIndex);
			if(pub) copyDefinitions(imported.definition(), definition, false, startIndex);
		}
		expectStmtEnd("Expected ',' or newline after use statement");
	}

	private void copyDefinitions(AmongDefinition from, AmongDefinition to, boolean report, int startIndex){
		from.macros().allMacros().forEach(m -> to.macros().add(m, report ? (t, s) -> report(t, s, startIndex) : null));
		from.operators().allOperators().forEach(o -> {
			OperatorRegistry.RegistrationResult r = to.operators().add(o);
			if(report&&!r.isSuccess()&&r!=OperatorRegistry.RegistrationResult.IDENTICAL_DUPLICATE)
				reportWarning("Cannot import operator definition '"+o+"':\n  "+r.message(o), startIndex);
		});
	}

	private void expectNext(Token.TokenType type){
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
		Token next = tokenizer.next(true, TokenizationMode.VALUE);
		if(!next.isLiteral()){
			reportError("Expected value");
			tokenizer.reset(true);
			return null;
		}
		AmongPrimitive p = Among.value(next.expectLiteral());
		return next.is(QUOTED_PRIMITIVE)||resolveParamRef(p) ? p : primitiveMacro(p, next.start);
	}

	private boolean resolveParamRef(Among target){
		return currentMacro!=null&&currentMacro.resolveParamRef(target);
	}

	@Nullable private Among nameable(boolean operation){
		tokenizer.discard();
		Token next = tokenizer.next(true, operation ? TokenizationMode.OPERATION : TokenizationMode.VALUE);
		switch(next.type){
			case L_BRACE: return obj(null);
			case L_BRACKET: return list(null);
			case L_PAREN:{
				AmongList o = oper(null);
				return o.size()==1 ? o.get(0) : o;
			}
			default:
				if(next.isLiteral()){
					// lookahead to find if it's nameable instance
					switch(tokenizer.next(operation, TokenizationMode.UNEXPECTED).type){
						case L_BRACE:{
							AmongObject o = obj(next.expectLiteral());
							return next.is(QUOTED_PRIMITIVE)||resolveParamRef(o) ? o : objectMacro(o, next.start);
						}
						case L_BRACKET:{
							AmongList l = list(next.expectLiteral());
							return next.is(QUOTED_PRIMITIVE)||resolveParamRef(l) ? l : listMacro(l, next.start);
						}
						case L_PAREN:{
							AmongList o = oper(next.expectLiteral());
							return next.is(QUOTED_PRIMITIVE)||resolveParamRef(o) ? o : operationMacro(o, next.start);
						}
						default: tokenizer.reset(true); return null;
					}
				}
				tokenizer.reset();
				return null;
		}
	}

	private AmongObject obj(@Nullable String name){
		AmongObject object = Among.namedObject(name);
		L:
		while(true){
			Token keyToken = tokenizer.next(true, TokenizationMode.KEY);
			switch(keyToken.type){
				case EOF: reportError("Unterminated object");
				case R_BRACE: break L;
				case COMMA: reportError("Redundant comma"); continue;
			}
			if(!keyToken.isLiteral()){
				reportError("Expected property key");
				if(tryToRecover(TokenizationMode.KEY, R_BRACE, true)) break;
				else continue;
			}

			tokenizer.discard();
			if(!tokenizer.next(true, TokenizationMode.UNEXPECTED).is(COLON)){
				reportError("Expected ':' after property key");
				tokenizer.reset();
				if(tryToRecover(TokenizationMode.UNEXPECTED, R_BRACE, true)) break;
				else continue;
			}
			String key = keyToken.expectLiteral();
			if(object.hasProperty(key))
				report(engine.allowDuplicateObjectProperty ? ReportType.WARN : ReportType.ERROR,
						"Property '"+key+"' is already defined", keyToken.start);

			Among expr = exprOrError();

			if(!object.hasProperty(key)) object.setProperty(key, expr);
			Token next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
			switch(next.type){
				case BR:
					tokenizer.discard();
					next = tokenizer.next(true, TokenizationMode.KEY);
					if(!next.is(Token.TokenType.COMMA)) tokenizer.reset();
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
			Token next = tokenizer.next(true, TokenizationMode.UNEXPECTED);
			switch(next.type){
				case EOF: reportError("Unterminated list");
				case R_BRACKET: break L;
				case COMMA: reportError("Redundant comma"); continue;
			}
			tokenizer.reset(next.is(ERROR));
			Among expr = expr();
			if(expr!=null) list.add(expr);
			next = tokenizer.next(false, TokenizationMode.UNEXPECTED);
			switch(next.type){
				case BR:
					tokenizer.discard();
					next = tokenizer.next(true, TokenizationMode.UNEXPECTED);
					if(!next.is(Token.TokenType.COMMA))
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
		list.setOperation(true);
		L:
		while(true){
			tokenizer.discard();
			switch(tokenizer.next(true, TokenizationMode.OPERATION).type){
				case EOF: reportError("Unterminated operation");
				case R_PAREN: break L;
				case COMMA: reportError("Redundant comma"); continue;
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
				case BINARY: return group.isRightAssociative() ? rightAssociativeBinary(operators, i) : binary(operators, i);
				case POSTFIX: return postfix(operators, i);
				case PREFIX: return prefix(operators, i);
				default: throw new IllegalStateException("Unreachable");
			}
		}
		// read primitive
		tokenizer.discard();
		Among a = nameable(true);
		if(a!=null) return a;
		tokenizer.reset();
		Token next = tokenizer.next(true, TokenizationMode.OPERATION);
		if(!next.isLiteral()){
			reportError("Expected value");
			tokenizer.reset();
			if(tryToRecover(TokenizationMode.UNEXPECTED, R_PAREN, true, true))
				tokenizer.reset();
			return Among.value("ERROR");
		}
		AmongPrimitive p = Among.value(next.expectLiteral());
		return next.is(QUOTED_PRIMITIVE)||resolveParamRef(p) ? p : primitiveMacro(p, next.start);
	}

	private Among binary(List<OperatorRegistry.PriorityGroup> operators, int i){
		Among a = operationExpression(operators, i+1);
		while(true){
			tokenizer.discard();
			Token next = tokenizer.next(true, TokenizationMode.OPERATION);
			if(next.isOperatorOrKeyword()){
				OperatorDefinition op = operators.get(i).get(next.expectLiteral());
				if(op!=null){
					Among b = operationExpression(operators, i+1);
					if(op.hasProperty(OperatorProperty.ACCESSOR)){
						if(b.isPrimitive()){
							a = accessMacro(Among.namedList(op.aliasOrName()+b.asPrimitive().getValue(), a), next.start);
						}else{
							AmongNameable b2 = b.asNameable().copy();
							b2.setName("");
							AmongList call = Among.namedList(op.aliasOrName()+b.asNameable().getName(), a, b2);
							a = b.isObj() ? objectFnMacro(call, next.start) :
									b.asList().isOperation() ? operationFnMacro(call, next.start) :
											listFnMacro(call, next.start);
						}
					}else{
						AmongList list = Among.namedList(op.aliasOrName(), a, b);
						list.setOperation(true);
						a = operationMacro(list, next.start);
					}
					continue;
				}
			}
			tokenizer.reset();
			return a;
		}
	}

	private Among rightAssociativeBinary(List<OperatorRegistry.PriorityGroup> operators, int i){
		Among a = operationExpression(operators, i+1);
		tokenizer.discard();
		Token next = tokenizer.next(true, TokenizationMode.OPERATION);
		if(next.isOperatorOrKeyword()){
			OperatorDefinition op = operators.get(i).get(next.expectLiteral());
			if(op!=null){
				AmongList list = Among.namedList(op.aliasOrName(), a, rightAssociativeBinary(operators, i));
				list.setOperation(true);
				return operationMacro(list, next.start);
			}
		}
		tokenizer.reset();
		return a;
	}

	private Among postfix(List<OperatorRegistry.PriorityGroup> operators, int i){
		Among a = operationExpression(operators, i+1);
		while(true){
			tokenizer.discard();
			Token next = tokenizer.next(true, TokenizationMode.OPERATION);
			if(next.isOperatorOrKeyword()){
				OperatorDefinition op = operators.get(i).get(next.expectLiteral());
				if(op!=null){
					AmongList list = Among.namedList(op.aliasOrName(), a);
					list.setOperation(true);
					a = operationMacro(list, next.start);
					continue;
				}
			}
			tokenizer.reset();
			return a;
		}
	}

	private Among prefix(List<OperatorRegistry.PriorityGroup> operators, int i){
		tokenizer.discard();
		Token next = tokenizer.next(true, TokenizationMode.OPERATION);
		if(next.isOperatorOrKeyword()){
			OperatorDefinition op = operators.get(i).get(next.expectLiteral());
			if(op!=null){
				AmongList list = Among.namedList(op.aliasOrName(), prefix(operators, i));
				list.setOperation(true);
				return operationMacro(list, next.start);
			}
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
	private Among accessMacro(AmongList call, int sourcePosition){
		return macro(call, call.getName(), MacroType.ACCESS, sourcePosition);
	}
	private Among objectFnMacro(AmongList call, int sourcePosition){
		return macro(call, call.getName(), MacroType.OBJECT_FN, sourcePosition);
	}
	private Among listFnMacro(AmongList call, int sourcePosition){
		return macro(call, call.getName(), MacroType.LIST_FN, sourcePosition);
	}
	private Among operationFnMacro(AmongList call, int sourcePosition){
		return macro(call, call.getName(), MacroType.OPERATION_FN, sourcePosition);
	}
	private Among macro(Among target, String macroName, MacroType macroType, int sourcePosition){
		MacroRegistry.Group g = importDefinition.macros().groupFor(macroName, macroType);
		if(g==null) return target;
		Macro macro = g.search(target, (t, s) -> report(t, s, sourcePosition));
		if(macro!=null){
			if(currentMacro!=null){
				currentMacro.resolveMacroCall(macro, target);
				return target;
			}
			try{
				Among among = macro.apply(target, engine.copyMacroConstant, (t, s) -> report(t, s, sourcePosition));
				if(among!=null) return among;
			}catch(RuntimeException ex){
				report(ReportType.ERROR, "Unexpected error on macro processing", sourcePosition, ex);
			}
		}
		return Among.value("ERROR");
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
		Token lastToken = tokenizer.lastToken();
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
	private boolean tryToRecover(TokenizationMode mode, @Nullable Token.TokenType closure, boolean returnOnComma){
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
	private boolean tryToRecover(TokenizationMode mode, @Nullable Token.TokenType closure, boolean returnOnComma, boolean returnOnLineBreak){
		boolean prevRecovering = this.recovering;
		this.recovering = true;
		while(true){
			tokenizer.discard();
			Token.TokenType t = tokenizer.next(false, mode).type;
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

	private final class ParsingMacro{
		private final int start;
		private final String name;
		private final MacroType type;
		private final List<MacroParameter> params = new ArrayList<>();
		private final List<Map.Entry<MacroOp, Among>> operationToTarget = new ArrayList<>();

		@Nullable private List<Inference> typeInferences = null;

		private boolean optionalParamSeen;
		private boolean invalid;

		private ParsingMacro(int start, String name, MacroType type){
			this.start = start;
			this.name = name;
			this.type = type;
			if(type.isFunctionMacro())
				params.add(new MacroParameter("self", null));
		}

		public void newParam(String name, @Nullable Among defaultValue, int pos){
			if(type==MacroType.CONST){
				reportError("Constant macros cannot have parameters");
				invalid = true;
			}else if(type==MacroType.ACCESS){
				reportError("Access macros cannot have parameters");
				invalid = true;
			}else if(paramIndex(name)>=0){
				reportError(type.isFunctionMacro()&&"self".equals(name) ?
						"Cannot define parameter named 'self' in function macros" :
						"Duplicated parameter '"+name+"'.", pos);
				invalid = true;
			}else{
				if(type==MacroType.LIST||type==MacroType.OPERATION||
						type==MacroType.LIST_FN||type==MacroType.OPERATION_FN){
					if(defaultValue==null){
						if(optionalParamSeen){
							reportError("Optional parameters of "+type.friendlyName()+
									" macro should be consecutive, placed at end of the parameter list", pos);
							invalid = true;
						}
					}else optionalParamSeen = true;
				}
				params.add(new MacroParameter(name, defaultValue));
			}
		}

		public int paramIndex(String name){
			for(int i = 0; i<params.size(); i++){
				MacroParameter p = params.get(i);
				if(p.name().equals(name)) return i;
			}
			return -1;
		}

		private Inference getTypeInference(int paramIndex){
			if(typeInferences==null)
				typeInferences = new ArrayList<>();
			else for(Inference i : typeInferences){
				if(i.index==paramIndex) return i;
			}
			Inference i = new Inference(paramIndex);
			typeInferences.add(i);
			return i;
		}

		public void inferTypeAs(int paramIndex, byte typeInference){
			Inference i = getTypeInference(paramIndex);
			if(i.type==0) return; // already reported
			MacroParameter p = params.get(paramIndex);

			byte newInference = (byte)(typeInference&i.type);
			if(i.type!=typeInference){
				if(newInference==0){
					reportWarning("Parameter '"+p.name()+"' has no valid input: needs to satisfy both "+
							TypeFlags.toString(i.type)+" AND "+TypeFlags.toString(typeInference));
				}else if(p.defaultValue()!=null&&!TypeFlags.matches(newInference, p.defaultValue())){
					reportWarning("Default value of the parameter '"+p.name()+"' is invalid");
				}
				i.type = newInference;
			}
		}

		public boolean resolveParamRef(Among target){
			String name;
			if(target.isPrimitive()) name = target.asPrimitive().getValue();
			else if(target.isNameable()) name = target.asNameable().getName();
			else return false;
			int i = paramIndex(name);
			if(i<0) return false;
			if(target.isNameable()) inferTypeAs(i, TypeFlags.PRIMITIVE);
			if(!invalid)
				operationToTarget.add(new SimpleEntry<>(
						target.isPrimitive() ?
								new ValueReplacement(i) :
								new NameReplacement(i),
						target));
			return true;
		}

		public void resolveMacroCall(Macro macro, Among target){
			operationToTarget.add(new SimpleEntry<>(new MacroCall(macro), target));
		}

		public void register(Among expr){
			if(invalid) return;
			invalid = true;
			List<MacroReplacement> replacements = new ArrayList<>(this.operationToTarget.size());
			expr.walk(new AmongWalker(){
				@Override public void walk(AmongPrimitive primitive, NodePath path){
					resolve(primitive, path);
				}
				@Override public void walkAfter(AmongObject object, NodePath path){
					resolve(object, path);
				}
				@Override public void walkAfter(AmongList list, NodePath path){
					resolve(list, path);
				}
				private void resolve(Among target, NodePath path){
					for(Iterator<Map.Entry<MacroOp, Among>> it = operationToTarget.iterator(); it.hasNext(); ){
						Map.Entry<MacroOp, Among> e = it.next();
						if(e.getValue()==target){
							replacements.add(new MacroReplacement(path, e.getKey()));
							it.remove();
							return;
						}
					}
				}
			});
			if(!operationToTarget.isEmpty()){
				reportError("Unresolved macro operation: "+
						operationToTarget.stream().map(e -> e.getKey().toString()).collect(Collectors.joining(", ")), start);
				return;
			}
			byte[] typeInferences;
			if(this.typeInferences!=null){
				typeInferences = new byte[params.size()];
				Arrays.fill(typeInferences, TypeFlags.ANY);
				for(Inference i : this.typeInferences){
					typeInferences[i.index] = i.type;
				}
			}else typeInferences = null;
			MacroDefinition macro = new MacroDefinition(new MacroSignature(name, type),
					type.isFunctionMacro() ?
							MacroParameterList.of(params.subList(1, params.size())) :
							MacroParameterList.of(params),
					expr, replacements, typeInferences);
			importDefinition.macros().add(macro, (t, s) -> report(t, s, start));
			definition.macros().add(macro);
		}
	}

	private static final class Inference{
		private final int index;
		private byte type = TypeFlags.ANY;

		private Inference(int index){
			this.index = index;
		}
	}

	private static final class TypeAndProperty{
		private final OperatorType type;
		private final byte properties;
		private final double priority;

		private TypeAndProperty(OperatorType type, byte properties, double priority){
			this.type = type;
			this.properties = properties;
			this.priority = priority;
		}
	}

	private enum OperatorPropertyEnum{
		LEFT_ASSOCIATIVE, RIGHT_ASSOCIATIVE, ACCESSOR;

		@Override public String toString(){
			switch(this){
				case LEFT_ASSOCIATIVE: return "left-associative";
				case RIGHT_ASSOCIATIVE: return "right-associative";
				case ACCESSOR: return "accessor";
				default: throw new IllegalStateException("Unreachable");
			}
		}
	}
}
