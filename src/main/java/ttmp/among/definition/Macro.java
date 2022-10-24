package ttmp.among.definition;

import org.jetbrains.annotations.Nullable;
import ttmp.among.compile.ReportType;
import ttmp.among.exception.Sussy;
import ttmp.among.format.PrettifyContext;
import ttmp.among.obj.Among;
import ttmp.among.obj.AmongList;
import ttmp.among.obj.AmongObject;
import ttmp.among.format.AmongUs;
import ttmp.among.format.PrettifyOption;
import ttmp.among.format.ToPrettyString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Base class for both user-defined macro and code-defined macro.
 */
public abstract class Macro extends ToPrettyString.Base{
	public static MacroBuilder builder(String name, MacroType type){
		return new MacroBuilder(name, type);
	}

	private final MacroSignature signature;
	private final MacroParameterList parameter;

	private final byte @Nullable [] typeInferences;

	protected Macro(MacroSignature signature, MacroParameterList parameter, byte @Nullable [] typeInferences){
		switch(signature.type()){
			case CONST: case ACCESS:
				if(!parameter.isEmpty())
					throw new Sussy("Constant definitions cannot have parameter");
				break;
			case LIST: case OPERATION:
			case LIST_FN: case OPERATION_FN:
				if(!parameter.hasConsecutiveOptionalParams())
					throw new Sussy("Optional parameters of "+signature.type().friendlyName()+
							" macro should be consecutive, placed at end of the parameter list");
		}
		this.signature = signature;
		this.parameter = parameter;
		this.typeInferences = typeInferences;
	}

	public final MacroSignature signature(){
		return signature;
	}
	public final String name(){
		return signature.name();
	}
	public final MacroType type(){
		return signature.type();
	}
	public final MacroParameterList parameter(){
		return parameter;
	}

	/**
	 * Applies this macro to given object. The argument object will not be modified; either new object or fixed
	 * 'constant' object will be given, based on context.<br>
	 * This method always return new instance of the object. If you do not expect the result to be modified afterwards,
	 * you may specify {@code copyConstant} to be {@code false} for both faster and memory-efficient operation.
	 *
	 * @param argument Argument object
	 * @return Among object with macro applied, or {@code null} if any error occurs
	 * @throws NullPointerException If {@code argument == null}. Note that if the macro is argument-independent, it might
	 *                              not throw an exception
	 * @throws RuntimeException     If an unexpected error occurs. The exception should be reported back as error.
	 * @see Macro#apply(Among, boolean)
	 * @see Macro#apply(Among, boolean, BiConsumer)
	 */
	public final Among apply(Among argument){
		return apply(argument, true);
	}

	/**
	 * Applies this macro to given object. The argument object will not be modified; either new object or fixed
	 * 'constant' object will be given, based on context.<br>
	 * The macro can fail by two ways: Either by returning {@code null} (expected failure), and throwing an exception
	 * (unexpected failure). If {@code null} is returned, relevant information is passed to {@code reportHandler}. As
	 * such, providing report handler is highly recommended.<br>
	 * If an exception is thrown, it will be not reported with {@code reportHandler}. Handling these error cases are
	 * recommended.<br>
	 * If {@code copyConstant} is {@code false}, and this macro is argument-independent, the returned instance may be
	 * shared between other places. As the instance is shared among macro itself and possibly many other places where
	 * macro is used, modifying the result will bring consequences. This is intentional design choice to enable users
	 * to avoid possibly expensive deep copy process on right situations. Set {@code copyConstant} to {@code true} if
	 * you expect the object to be modified afterwards; otherwise you may set {@code copyConstant} to {@code false} for
	 * both faster and memory-efficient operation.
	 *
	 * @param argument     Argument object
	 * @param copyConstant If {@code true}, constant macro will return deep copy of template.
	 * @return Among object with macro applied, or {@code null} if any error occurs. If the macro is argument-independent,
	 * the returned instance may be shared between other places, including the macro itself.
	 * @throws NullPointerException If {@code argument == null}. Note that if the macro is argument-independent, it might
	 *                              not throw an exception
	 * @throws RuntimeException     If an unexpected error occurs. The exception should be reported back as error.
	 * @see Macro#apply(Among, boolean, BiConsumer)
	 */
	@Nullable public final Among apply(Among argument, boolean copyConstant){
		return apply(argument, copyConstant, null);
	}

	/**
	 * Applies this macro to given object. The argument object will not be modified; either new object or fixed
	 * 'constant' object will be given, based on context.<br>
	 * The macro can fail by two ways: Either by returning {@code null} (expected failure), and throwing an exception
	 * (unexpected failure). If {@code null} is returned, relevant information is passed to {@code reportHandler}. As
	 * such, providing report handler is highly recommended.<br>
	 * If an exception is thrown, it will be not reported with {@code reportHandler}. Handling these error cases are
	 * recommended.<br>
	 * If {@code copyConstant} is {@code false}, and this macro is argument-independent, the returned instance may be
	 * shared between other places. As the instance is shared among macro itself and possibly many other places where
	 * macro is used, modifying the result will bring consequences. This is intentional design choice to enable users
	 * to avoid possibly expensive deep copy process on right situations. Set {@code copyConstant} to {@code true} if
	 * you expect the object to be modified afterwards; otherwise you may set {@code copyConstant} to {@code false} for
	 * both faster and memory-efficient operation.
	 *
	 * @param argument      Argument object
	 * @param copyConstant  If {@code true}, constant macro will return deep copy of template.
	 * @param reportHandler Optional report handler for analyzing any compilation issues. Presence of the report handler
	 *                      does not change process.
	 * @return Among object with macro applied, or {@code null} if any 'expected' error occurs. If the macro is
	 * argument-independent, the returned instance may be shared between other places, including the macro itself.
	 * @throws NullPointerException If {@code argument == null}. Note that if the macro is argument-independent, it might
	 *                              not throw an exception
	 * @throws RuntimeException     If an unexpected error occurs. The exception should be reported back as error.
	 */
	@Nullable public final Among apply(Among argument, boolean copyConstant, @Nullable BiConsumer<ReportType, String> reportHandler){
		Among[] args = toArgs(argument, reportHandler);
		if(args==null) return null;
		if(typeInferences!=null){
			boolean invalid = false;
			for(int i = 0, j = Math.min(args.length, this.typeInferences.length); i<j; i++){
				if(!TypeFlags.matches(this.typeInferences[i], args[i])){
					if(reportHandler!=null){
						int actualParamIndex = type().isFunctionMacro() ? i-1 : i;
						String paramName = actualParamIndex>=0&&actualParamIndex<this.parameter.size() ?
								this.parameter.paramAt(actualParamIndex).name() : "self";
						reportHandler.accept(ReportType.ERROR,
								"Type of argument '"+paramName+"' does not match its inferred type.\n" +
										"  Expected type: "+TypeFlags.toString(this.typeInferences[i])+"\n" +
										"  Supplied argument: "+TypeFlags.toString(TypeFlags.from(args[i])));
					}
					invalid = true;
				}
			}
			if(invalid) return null;
		}
		return applyMacro(args, copyConstant, reportHandler);
	}
	@Nullable protected abstract Among applyMacro(Among[] args, boolean copyConstant, @Nullable BiConsumer<ReportType, String> reportHandler);

	@Nullable private Among[] toArgs(Among argument, @Nullable BiConsumer<ReportType, String> reportHandler){
		switch(this.type()){
			case CONST: return new Among[0];
			case OBJECT: return objectMacro(argument, reportHandler, null);
			case LIST: case OPERATION: return listMacro(argument, reportHandler, null);
			case ACCESS: case OBJECT_FN: case LIST_FN: case OPERATION_FN:
				if(argument.isList()){
					AmongList l = argument.asList();
					int requiredSize = this.type()==MacroType.ACCESS ? 1 : 2;
					if(l.size()>=requiredSize){
						if(l.size()>requiredSize&&reportHandler!=null)
							reportHandler.accept(ReportType.WARN, "Unused function parameters: "+l.size()+" provided");
						Among self = l.get(0);
						return this.type()==MacroType.ACCESS ? new Among[]{self} :
								this.type()==MacroType.OBJECT_FN ? objectMacro(l.get(1), reportHandler, self) :
										listMacro(l.get(1), reportHandler, self);
					}
				}
				if(reportHandler!=null) reportHandler.accept(ReportType.ERROR, this.type()==MacroType.ACCESS ?
						"Expected 'self' as argument" :
						"Expected pair of 'self' and 'args' as argument");
				return null;
			default: throw new IllegalStateException("Unreachable");
		}
	}

	@Nullable private Among[] objectMacro(Among argument, @Nullable BiConsumer<ReportType, String> reportHandler, @Nullable Among self){
		if(!argument.isObj()){
			if(reportHandler!=null) reportHandler.accept(ReportType.ERROR, "Expected object as argument");
			return null;
		}
		AmongObject o = argument.asObj();
		List<Among> args = new ArrayList<>();
		if(self!=null) args.add(self);
		for(int i = 0; i<this.parameter().size(); i++){
			MacroParameter p = this.parameter().paramAt(i);
			Among val = o.getProperty(p.name());
			if(val==null){
				if(p.defaultValue()!=null) val = p.defaultValue();
				else{
					if(reportHandler!=null) reportHandler.accept(ReportType.ERROR, "Missing argument '"+p.name()+'\'');
					else return null;
					args = null;
				}
			}
			if(args!=null) args.add(val);
		}
		if(reportHandler!=null)
			for(String key : argument.asObj().properties().keySet())
				if(this.parameter().indexOf(key)==-1)
					reportHandler.accept(ReportType.WARN, "Unused argument '"+key+'\'');
		return args!=null ? args.toArray(new Among[0]) : null;
	}

	@Nullable private Among[] listMacro(Among argument, @Nullable BiConsumer<ReportType, String> reportHandler, @Nullable Among self){
		if(!argument.isList()){
			if(reportHandler!=null) reportHandler.accept(ReportType.ERROR, "Expected list as argument");
			return null;
		}
		AmongList l = argument.asList();
		if(l.size()<parameter().requiredParameters()){
			if(reportHandler!=null) reportHandler.accept(ReportType.ERROR,
					"Not enough parameters: minimum of "+parameter().requiredParameters()+" expected, "+l.size()+" provided");
			return null;
		}else{
			if(l.size()>parameter().size()&&reportHandler!=null){
				reportHandler.accept(ReportType.WARN,
						"Unused parameters: maximum of "+parameter().size()+" expected, "+l.size()+" provided");
			}
			List<Among> args = new ArrayList<>();
			if(self!=null) args.add(self);
			for(int i = 0; i<parameter().size(); i++)
				args.add(i<l.size() ?
						l.get(i) :
						Objects.requireNonNull(parameter().paramAt(i).defaultValue()));
			return args.toArray(new Among[0]);
		}
	}

	@Override public void toString(StringBuilder stb, PrettifyOption option, PrettifyContext context){
		stb.append(type().isFunctionMacro() ? "fn " : "macro ");
		signatureAndParameter().toString(stb, option, context);
		macroBodyToString(stb.append(':'), option, context);
	}
	@Override public void toPrettyString(StringBuilder stb, int indents, PrettifyOption option, PrettifyContext context){
		stb.append(type().isFunctionMacro() ? "fn " : "macro ");
		signatureAndParameter().toPrettyString(stb, indents, option, context);
		macroBodyToPrettyString(stb.append(" : "), indents, option, context);
	}
	protected abstract void macroBodyToString(StringBuilder stb, PrettifyOption option, PrettifyContext context);
	protected abstract void macroBodyToPrettyString(StringBuilder stb, int indents, PrettifyOption option, PrettifyContext context);

	public ToPrettyString signatureAndParameter(){
		return signatureAndParameter(false);
	}
	public ToPrettyString signatureAndParameter(boolean replaceDefaultValueWithStubs){
		return new ToPrettyString(){
			@Override public String toString(){
				return toString(PrettifyOption.DEFAULT);
			}
			@Override public void toString(StringBuilder stb, PrettifyOption option, PrettifyContext context){
				if(AmongUs.isSimpleMacroName(name())) AmongUs.simpleMacroNameToString(stb, name());
				else AmongUs.primitiveToString(stb, name());
				switch(type()){
					case OBJECT: case OBJECT_FN: stb.append('{'); break;
					case LIST: case LIST_FN: stb.append('['); break;
					case OPERATION: case OPERATION_FN: stb.append('('); break;
					default: return;
				}
				parameter.toString(stb, option, PrettifyContext.NONE);
				switch(type()){
					case OBJECT: case OBJECT_FN: stb.append('}'); break;
					case LIST: case LIST_FN: stb.append(']'); break;
					case OPERATION: case OPERATION_FN: stb.append(')'); break;
				}
			}
			@Override public void toPrettyString(StringBuilder stb, int indents, PrettifyOption option, PrettifyContext context){
				if(AmongUs.isSimpleMacroName(name())) AmongUs.simpleMacroNameToString(stb, name());
				else AmongUs.primitiveToPrettyString(stb, name(), indents, option);
				switch(type()){
					case OBJECT: case OBJECT_FN: stb.append('{'); break;
					case LIST: case LIST_FN: stb.append('['); break;
					case OPERATION: case OPERATION_FN: stb.append('('); break;
					default: return;
				}
				parameter.toPrettyString(stb, indents, option, replaceDefaultValueWithStubs);
				switch(type()){
					case OBJECT: case OBJECT_FN: stb.append('}'); break;
					case LIST: case LIST_FN: stb.append(']'); break;
					case OPERATION: case OPERATION_FN: stb.append(')'); break;
				}
			}
		};
	}
}
