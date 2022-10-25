package ttmp.among.definition;

import org.jetbrains.annotations.Nullable;
import ttmp.among.compile.ReportType;
import ttmp.among.exception.Sussy;
import ttmp.among.format.PrettifyOption;
import ttmp.among.obj.Among;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Builder for {@link MacroDefinition}.
 */
public final class MacroBuilder{
	private final String name;
	private final MacroType type;
	private final List<MacroParameter> parameters = new ArrayList<>();
	@Nullable private List<Byte> typeInferences;

	public MacroBuilder(String name, MacroType type){
		this.name = Objects.requireNonNull(name);
		this.type = Objects.requireNonNull(type);
	}

	public MacroBuilder param(String paramName){
		return param(paramName, null);
	}
	public MacroBuilder param(String paramName, @Nullable Among defaultValue){
		return param(paramName, defaultValue, TypeFlags.ANY);
	}
	public MacroBuilder param(String paramName, int typeInference){
		return param(paramName, null, (byte)typeInference);
	}
	public MacroBuilder param(String paramName, byte typeInference){
		return param(paramName, null, typeInference);
	}
	public MacroBuilder param(String paramName, @Nullable Among defaultValue, int typeInference){
		return param(paramName, defaultValue, (byte)typeInference);
	}
	public MacroBuilder param(String paramName, @Nullable Among defaultValue, byte typeInference){
		typeInference = TypeFlags.normalize(typeInference);
		if(typeInference==0) throw new Sussy("Type inference of parameter '"+paramName+"' has no valid input");
		if(typeInference!=TypeFlags.ANY) createTypeInference();
		parameters.add(new MacroParameter(paramName, defaultValue));
		if(typeInferences!=null) typeInferences.add(typeInference);
		return this;
	}

	public MacroBuilder inferSelfType(int typeInference){
		return inferSelfType((byte)typeInference);
	}
	public MacroBuilder inferSelfType(byte typeInference){
		if(!type.isFunctionMacro()) throw new Sussy("Cannot infer self type of non-function macros");
		typeInference = TypeFlags.normalize(typeInference);
		if(typeInference==0) throw new Sussy("Type inference of self has no valid input");
		createTypeInference();
		Objects.requireNonNull(typeInferences).set(0, typeInference);
		return this;
	}

	private void createTypeInference(){
		if(typeInferences==null){
			typeInferences = new ArrayList<>();
			for(int i = type.isFunctionMacro() ? -1 : 0; i<this.parameters.size(); i++)
				this.typeInferences.add(TypeFlags.ANY);
		}
	}

	private byte @Nullable [] buildTypeInference(){
		if(this.typeInferences==null) return null;
		byte[] typeInferences = new byte[this.typeInferences.size()];
		for(int i = 0; i<this.typeInferences.size(); i++)
			typeInferences[i] = this.typeInferences.get(i);
		return typeInferences;
	}

	/**
	 * Build a replacement-based macro - the one you define with Among script.
	 *
	 * @return New macro definition
	 * @throws Sussy If one of the arguments are unspecified or invalid
	 */
	public MacroDefinition build(Among template, MacroReplacement... replacements){
		return new MacroDefinition(new MacroSignature(name, type), MacroParameterList.of(parameters),
				template, Arrays.asList(replacements), buildTypeInference());
	}

	/**
	 * Build a code-defined macro with given function.
	 *
	 * @param function Function of the macro
	 * @return New macro definition
	 * @throws Sussy If one of the arguments are unspecified or invalid
	 */
	public CustomMacro build(MacroFunction function){
		return new CustomMacro(new MacroSignature(name, type), MacroParameterList.of(parameters),
				function, buildTypeInference());
	}

	@FunctionalInterface
	public interface MacroFunction{
		/**
		 * Applies this macro to given object. The arguments should not be modified. The returning object may or may not
		 * be shared instance, based on context.<br>
		 * The function can fail by two ways: Either by returning {@code null} (expected failure), and throwing an exception
		 * (unexpected failure). If {@code null} is to be returned, relevant information should be passed to
		 * {@code reportHandler}.<br>
		 * If an exception is to be thrown, it is unnecessary to report the error to {@code reportHandler}.<br>
		 * If {@code copyConstant} is {@code false}, and this macro function is argument-independent, the returned instance
		 * may be shared between other places. As the instance is shared among macro itself and possibly many other places
		 * where macro is used, modifying the result will bring consequences. This is intentional design choice to enable
		 * users to avoid possibly expensive deep copy process on right situations.
		 *
		 * @param args          List of arguments. If the macro is non-function macro, size of the arguments are equal to
		 *                      parameter size, corresponding to each parameter by declaration order.<br>
		 *                      If the macro is function macro, size of the arguments are size of the parameter plus one,
		 *                      with {@code self} object being at the start followed by rest of the parameters in
		 *                      declaration order.
		 * @param copyConstant  If {@code true}, constant macro will return deep copy of template.
		 * @param reportHandler Optional report handler for analyzing any compilation issues. Presence of the report handler
		 *                      does not change process.
		 * @return Among object with macro applied, or {@code null} if any 'expected' error occurs. If the macro is
		 * argument-independent, the returned instance may be shared between other places, including the macro itself.
		 * @throws NullPointerException If {@code argument == null}. Note that if the macro is argument-independent, it might
		 *                              not throw an exception
		 * @throws RuntimeException     If an unexpected error occurs. The exception should be reported back as error.
		 */
		@Nullable Among applyMacro(Among[] args, boolean copyConstant, @Nullable BiConsumer<ReportType, String> reportHandler);
	}

	public static final class CustomMacro extends Macro{
		private final MacroFunction function;

		public CustomMacro(MacroSignature signature, MacroParameterList parameter, MacroFunction function, byte @Nullable [] typeInferences){
			super(signature, parameter, typeInferences);
			this.function = Objects.requireNonNull(function);
		}

		@Override @Nullable protected Among applyMacro(Among[] args, boolean copyConstant, @Nullable BiConsumer<ReportType, String> reportHandler){
			return function.applyMacro(args, copyConstant, reportHandler);
		}
		@Override protected void macroBodyToString(StringBuilder stb, PrettifyOption option){
			stb.append("Stub");
		}
		@Override protected void macroBodyToPrettyString(StringBuilder stb, int indents, PrettifyOption option){
			stb.append("Stub /*code-defined macro*/");
		}
	}
}
