package ttmp.among.macro;

import org.jetbrains.annotations.Nullable;
import ttmp.among.compile.ReportType;
import ttmp.among.exception.Sussy;
import ttmp.among.PrettifyContext;
import ttmp.among.PrettifyOption;
import ttmp.among.ToPrettyString;
import ttmp.among.obj.Among;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Macro definitions. Snippet below shows macros with each type written in Among.
 * <pre>
 * macro macro : "Hello!"
 * macro macro{} : "Hello!"
 * macro macro[] : "Hello!"
 * macro macro() : "Hello!"
 * </pre>
 * <p>
 * Note that, due to the nature of replacement operations, the results of {@link MacroDefinition#toString()}
 * and {@link ToPrettyString#toPrettyString(int, PrettifyOption, PrettifyContext)} might not produce re-compilable macro script.
 */
public final class MacroDefinition extends Macro{
	private final Among template;
	private final List<MacroReplacement> replacements;

	/**
	 * Creates new macro definition.
	 *
	 * @param sig      Signature of the macro
	 * @param params   Parameters of the macro
	 * @param template Result object of the macro; valid parameter references will be marked for replacements
	 * @throws NullPointerException If either of the parameters are {@code null}
	 * @throws Sussy                If one of the arguments are invalid
	 */
	public MacroDefinition(MacroSignature sig, MacroParameterList params, Among template, List<MacroReplacement> replacements, byte[] typeInferences){
		super(sig, params, typeInferences);
		this.template = Objects.requireNonNull(template);
		this.replacements = new ArrayList<>(replacements);
		for(MacroReplacement r : this.replacements) Objects.requireNonNull(r);
	}

	/**
	 * Returns deep copy of the raw template used in this macro. All parameter references will be gone. This method is strictly for debugging purposes.
	 */
	public Among template(){
		return template.copy();
	}

	public List<MacroReplacement> replacements(){
		return replacements;
	}

	/**
	 * Whether this macro has a characteristic of being a constant macro.<br>
	 * A macro is considered 'constant' when no element is modified with parameter. Macro with no parameter is always
	 * constant.<br>
	 * Not to be confused with {@link MacroType#CONST}.
	 *
	 * @return Whether this macro has a characteristic of being a constant macro
	 */
	public boolean isConstant(){
		return replacements.isEmpty();
	}

	@Override protected Among applyMacro(Among[] args, boolean copyConstant, @Nullable BiConsumer<ReportType, String> reportHandler){
		if(isConstant()) return copyConstant ? template.copy() : template;
		Among o = template.copy();
		for(MacroReplacement r : replacements)
			o = r.apply(args, o, copyConstant, reportHandler);
		return o;
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		MacroDefinition that = (MacroDefinition)o;
		return signature().equals(that.signature())&&parameter().equals(that.parameter())&&template.equals(that.template);
	}
	@Override public int hashCode(){
		return Objects.hash(signature(), parameter(), template);
	}

	@Override protected void macroBodyToString(StringBuilder stb, PrettifyOption option){
		template.toString(stb, option, PrettifyContext.NONE);
	}
	@Override protected void macroBodyToPrettyString(StringBuilder stb, int indents, PrettifyOption option){
		template.toPrettyString(stb, indents, option, PrettifyContext.NONE);
	}
}
