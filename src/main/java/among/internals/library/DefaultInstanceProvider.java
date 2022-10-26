package among.internals.library;

import among.AmongDefinition;
import among.AmongEngine;
import among.Provider;
import among.ReportType;
import among.RootAndDefinition;
import among.TypeFlags;
import among.macro.Macro;
import among.macro.MacroType;
import among.obj.Among;
import among.obj.AmongList;
import among.obj.AmongNameable;
import among.obj.AmongObject;
import among.operator.OperatorPriorities;
import among.operator.OperatorProperty;
import among.operator.OperatorRegistry;
import among.operator.OperatorType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Provider for "native files" that can be imported from all among scripts. This provider is automatically registered on
 * all instance of {@link AmongEngine}s.
 */
public final class DefaultInstanceProvider implements Provider<RootAndDefinition>{
	public static final String DEFAULT_OPERATOR = "default_operator";
	public static final String DEFAULT_OPERATORS = "default_operators";
	public static final String EVAL = "eval";
	public static final String COLLECTION = "collection";
	public static final String COLLECTIONS = "collections";
	public static final String FORMAT = "format";

	private DefaultInstanceProvider(){}
	private static final DefaultInstanceProvider INSTANCE = new DefaultInstanceProvider();
	public static DefaultInstanceProvider instance(){
		return INSTANCE;
	}

	@Nullable @Override public RootAndDefinition resolve(String path){
		switch(path){
			case DEFAULT_OPERATOR: case DEFAULT_OPERATORS:
				return new RootAndDefinition(defaultOperators());
			case EVAL:
				return new RootAndDefinition(eval());
			case COLLECTION: case COLLECTIONS:
				return new RootAndDefinition(collection());
			case FORMAT:
				return new RootAndDefinition(format());
			default: return null;
		}
	}

	/**
	 * Create a new definition with default set of operators.
	 *
	 * @return New definition with default operators
	 * @see <a href="https://github.com/AmongLang/Among/wiki/Operators-and-Keywords#default-operators">Default
	 * Operators @ Online Docs</a>
	 */
	public static AmongDefinition defaultOperators(){
		AmongDefinition definition = new AmongDefinition();
		OperatorRegistry o = definition.operators();
		o.addOperator("=", OperatorType.BINARY, OperatorProperty.RIGHT_ASSOCIATIVE, OperatorPriorities.BINARY_ASSIGN);
		o.addOperator("||", OperatorType.BINARY, OperatorPriorities.BINARY_LOGICAL_OR);
		o.addOperator("&&", OperatorType.BINARY, OperatorPriorities.BINARY_LOGICAL_AND);
		o.addOperator("==", OperatorType.BINARY, OperatorPriorities.BINARY_LOGICAL_EQUALITY);
		o.addOperator("!=", OperatorType.BINARY, OperatorPriorities.BINARY_LOGICAL_EQUALITY);
		o.addOperator(">", OperatorType.BINARY, OperatorPriorities.BINARY_LOGICAL_COMPARE);
		o.addOperator("<", OperatorType.BINARY, OperatorPriorities.BINARY_LOGICAL_COMPARE);
		o.addOperator(">=", OperatorType.BINARY, OperatorPriorities.BINARY_LOGICAL_COMPARE);
		o.addOperator("<=", OperatorType.BINARY, OperatorPriorities.BINARY_LOGICAL_COMPARE);
		o.addOperator("|", OperatorType.BINARY, OperatorPriorities.BINARY_BITWISE);
		o.addOperator("&", OperatorType.BINARY, OperatorPriorities.BINARY_BITWISE);
		o.addOperator("+", OperatorType.BINARY, OperatorPriorities.BINARY_ARITHMETIC_ADDITION);
		o.addOperator("-", OperatorType.BINARY, OperatorPriorities.BINARY_ARITHMETIC_ADDITION);
		o.addOperator("*", OperatorType.BINARY, OperatorPriorities.BINARY_ARITHMETIC_PRODUCT);
		o.addOperator("/", OperatorType.BINARY, OperatorPriorities.BINARY_ARITHMETIC_PRODUCT);
		o.addOperator("^", OperatorType.BINARY, OperatorPriorities.BINARY_ARITHMETIC_POWER);
		o.addOperator("**", OperatorType.BINARY, OperatorPriorities.BINARY_ARITHMETIC_POWER);
		o.addOperator("!", OperatorType.PREFIX, OperatorPriorities.PREFIX);
		o.addOperator("-", OperatorType.PREFIX, OperatorPriorities.PREFIX);
		o.addOperator("+", OperatorType.PREFIX, OperatorPriorities.PREFIX);
		o.addOperator(".", OperatorType.BINARY, "", OperatorProperty.ACCESSOR, OperatorPriorities.BINARY_ACCESS);
		return definition;
	}

	/**
	 * Create new definition with eval function.
	 *
	 * @return New definition with eval function
	 */
	public static AmongDefinition eval(){
		AmongDefinition definition = defaultOperators();
		definition.macros().add(Macro.builder("eval", MacroType.OPERATION)
				.param("expr")
				.build((args, copyConstant, reportHandler) -> EvalLib.eval(args[0], reportHandler)));
		return definition;
	}

	public static AmongDefinition collection(){
		AmongDefinition definition = new AmongDefinition();
		definition.operators().addOperator(".", OperatorType.BINARY, "", OperatorProperty.ACCESSOR, OperatorPriorities.BINARY_ACCESS);
		definition.macros().add(Macro.builder("named", MacroType.OPERATION_FN)
				.param("name", TypeFlags.PRIMITIVE)
				.inferSelfType(TypeFlags.NAMEABLE)
				.build((args, copyConstant, reportHandler) -> {
					AmongNameable copy = args[0].asNameable().copy();
					copy.setName(args[1].asPrimitive().getValue());
					return copy;
				}));
		definition.macros().add(Macro.builder("name", MacroType.ACCESS)
				.inferSelfType(TypeFlags.NAMEABLE)
				.build((args, copyConstant, reportHandler) -> Among.value(args[0].asNameable().getName())));
		definition.macros().add(Macro.builder("size", MacroType.ACCESS)
				.inferSelfType(TypeFlags.NAMEABLE)
				.build((args, copyConstant, reportHandler) -> Among.value(args[0].isObj() ?
						args[0].asObj().size() : args[0].asList().size())));
		definition.macros().add(Macro.builder("keys", MacroType.ACCESS)
				.inferSelfType(TypeFlags.OBJECT)
				.build((args, copyConstant, reportHandler) ->
						Among.list(args[0].asObj().properties().keySet().toArray())));
		definition.macros().add(Macro.builder("values", MacroType.ACCESS)
				.inferSelfType(TypeFlags.OBJECT)
				.build((args, copyConstant, reportHandler) ->
						Among.list(args[0].asObj().properties().values().toArray())));
		definition.macros().add(Macro.builder("properties", MacroType.ACCESS)
				.inferSelfType(TypeFlags.OBJECT)
				.build((args, copyConstant, reportHandler) ->
						Among.list(args[0].asObj()
								.properties().entrySet().stream()
								.map(e -> Among.list(e.getKey(), e.getValue()))
								.toArray())));
		definition.macros().add(Macro.builder("concat", MacroType.OPERATION_FN)
				.param("other", TypeFlags.LIST|TypeFlags.OPERATION)
				.inferSelfType(TypeFlags.LIST|TypeFlags.OPERATION)
				.build((args, copyConstant, reportHandler) -> {
					AmongList copy = args[0].asList().copy();
					for(Among a : args[1].asList()) copy.add(a);
					return copy;
				}));
		definition.macros().add(Macro.builder("merge", MacroType.OPERATION_FN)
				.param("other", TypeFlags.OBJECT)
				.inferSelfType(TypeFlags.OBJECT)
				.build((args, copyConstant, reportHandler) -> {
					AmongObject copy = args[0].asObj().copy();
					for(Map.Entry<String, Among> e : args[1].asObj().properties().entrySet()){
						if(!copy.hasProperty(e.getKey())) copy.setProperty(e.getKey(), e.getValue());
					}
					return copy;
				}));
		definition.macros().add(Macro.builder("get", MacroType.OPERATION_FN)
				.param("index", TypeFlags.PRIMITIVE)
				.inferSelfType(TypeFlags.NAMEABLE)
				.build((args, copyConstant, reportHandler) -> {
					if(args[0].isObj()){
						String key = args[1].asPrimitive().getValue();
						Among a = args[0].asObj().getProperty(key);
						if(a==null&&reportHandler!=null)
							reportHandler.accept(ReportType.ERROR, "No property '"+key+"' in object");
						return a;
					}else{
						AmongList l = args[0].asList();
						try{
							int i = args[1].asPrimitive().getIntValue();
							if(i>=0&&i<l.size()) return l.get(i);
							if(reportHandler!=null)
								reportHandler.accept(ReportType.ERROR, "Index out of range ("+i+", size = "+l.size()+")");
						}catch(NumberFormatException ex){
							if(reportHandler!=null) reportHandler.accept(ReportType.ERROR, "Expected int");
						}
					}
					return null;
				}));
		definition.macros().add(Macro.builder("getOrDefault", MacroType.OPERATION_FN)
				.param("index", TypeFlags.PRIMITIVE)
				.param("default")
				.inferSelfType(TypeFlags.NAMEABLE)
				.build((args, copyConstant, reportHandler) -> {
					if(args[0].isObj()){
						String key = args[1].asPrimitive().getValue();
						Among a = args[0].asObj().getProperty(key);
						return a!=null ? a : args[2];
					}else try{
						int i = args[1].asPrimitive().getIntValue();
						AmongList l = args[0].asList();
						return i>=0&&i<l.size() ? l.get(i) : args[2];
					}catch(NumberFormatException ex){
						if(reportHandler!=null) reportHandler.accept(ReportType.ERROR, "Expected int");
						return null;
					}
				}));
		definition.macros().add(Macro.builder("add", MacroType.OPERATION_FN)
				.param("value")
				.inferSelfType(TypeFlags.LIST|TypeFlags.OPERATION)
				.build((args, copyConstant, reportHandler) -> {
					AmongList l = args[0].asList().copy();
					l.add(args[1]);
					return l;
				}));
		definition.macros().add(Macro.builder("set", MacroType.OPERATION_FN)
				.param("index", TypeFlags.PRIMITIVE)
				.param("value")
				.inferSelfType(TypeFlags.NAMEABLE)
				.build((args, copyConstant, reportHandler) -> {
					if(args[0].isObj()){
						AmongObject o = args[0].asObj().copy();
						o.setProperty(args[1].asPrimitive().getValue(), args[2]);
						return o;
					}else try{
						int i = args[1].asPrimitive().getIntValue();
						if(i>=0&&i<args[0].asList().size()){
							AmongList l = args[0].asList().copy();
							l.set(i, args[2]);
							return l;
						}
						if(reportHandler!=null)
							reportHandler.accept(ReportType.ERROR, "Index out of range ("+i+", size = "+args[0].asList().size()+")");
					}catch(NumberFormatException ex){
						if(reportHandler!=null) reportHandler.accept(ReportType.ERROR, "Expected int");
					}
					return null;
				}));
		definition.macros().add(Macro.builder("remove", MacroType.OPERATION_FN)
				.param("index", TypeFlags.PRIMITIVE)
				.inferSelfType(TypeFlags.NAMEABLE)
				.build((args, copyConstant, reportHandler) -> {
					if(args[0].isObj()){
						String key = args[1].asPrimitive().getValue();
						if(!args[0].asObj().hasProperty(key)) return args[0];
						AmongObject o = args[0].asObj().copy();
						o.removeProperty(key);
						return o;
					}else try{
						int i = args[1].asPrimitive().getIntValue();
						if(i>=0&&i<args[0].asList().size()){
							AmongList l = args[0].asList().copy();
							l.removeAt(i);
							return l;
						}
						if(reportHandler!=null)
							reportHandler.accept(ReportType.ERROR, "Index out of range ("+i+", size = "+args[0].asList().size()+")");
					}catch(NumberFormatException ex){
						if(reportHandler!=null) reportHandler.accept(ReportType.ERROR, "Expected int");
					}
					return null;
				}));
		return definition;
	}

	public static AmongDefinition format(){
		AmongDefinition definition = new AmongDefinition();
		definition.operators().addOperator("%", OperatorType.BINARY, "format", 0.5);
		definition.macros().add(Macro.builder("format", MacroType.OPERATION)
				.param("format", TypeFlags.PRIMITIVE)
				.param("argument", Among.list())
				.build((args, copyConstant, reportHandler) -> {
					String fmt = args[0].asPrimitive().getValue();
					Among a = args[1];
					return Among.value(a.isPrimitive() ? FormatLib.format(fmt, a.asPrimitive().getValue()) :
							a.isList() ? FormatLib.format(fmt, a.asList().values().toArray()) :
									FormatLib.format(fmt, a.asObj().properties()));
				}));
		return definition;
	}
}
