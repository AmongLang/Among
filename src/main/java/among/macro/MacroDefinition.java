package among.macro;

import among.ReportType;
import among.ToPrettyString;
import among.ToStringContext;
import among.ToStringOption;
import among.exception.Sussy;
import among.obj.Among;
import org.jetbrains.annotations.Nullable;

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
 * and {@link ToPrettyString#toPrettyString(int, ToStringOption, ToStringContext)} might not produce re-compilable macro
 * script.
 */
public final class MacroDefinition extends Macro{
	private final Among template;
	private final List<MacroReplacement> replacements;

	/**
	 * Creates new macro definition.
	 *
	 * @param sig            Signature of the macro
	 * @param params         Parameters of the macro
	 * @param template       Result object of the macro; valid parameter references will be marked for replacements
	 * @param replacements   List of replacement operations
	 * @param typeInferences Optional type inferences for argument validation
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
	 * Returns deep copy of the raw template used in this macro. This method is strictly for debugging purposes.
	 *
	 * @return Deep copy of template object
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

	@Override protected void macroBodyToString(StringBuilder stb, ToStringOption option){
		template.toString(stb, option, ToStringContext.NONE);
	}
	@Override protected void macroBodyToPrettyString(StringBuilder stb, int indents, ToStringOption option){
		template.toPrettyString(stb, indents, option, ToStringContext.NONE);
	}
}
