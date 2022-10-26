package ttmp.among.macro;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ttmp.among.ToPrettyString;
import ttmp.among.ToStringContext;
import ttmp.among.ToStringOption;
import ttmp.among.internals.LiteralFormats;
import ttmp.among.obj.Among;

import java.util.Objects;

/**
 * Parameter of the {@link MacroDefinition} - name, and default value(optional).
 *
 * @see MacroDefinition
 * @see MacroParameterList
 */
public final class MacroParameter extends ToPrettyString.Base implements Comparable<MacroParameter>{
	private final String name;
	@Nullable private final Among defaultValue;

	/**
	 * Creates a new instance of macro parameter.
	 *
	 * @param name         Name of the parameter
	 * @param defaultValue Default value of the parameter. Value of {@code null} indicates the parameter is not
	 *                     optional.
	 */
	public MacroParameter(String name, @Nullable Among defaultValue){
		this.name = name;
		this.defaultValue = defaultValue;
	}

	/**
	 * @return Name of this parameter.
	 */
	public String name(){
		return name;
	}
	/**
	 * @return Default value of this parameter. Value of {@code null} indicates the parameter is not optional.
	 */
	@Nullable public Among defaultValue(){
		return defaultValue;
	}

	@Override public int compareTo(@NotNull MacroParameter o){
		return name().compareTo(o.name());
	}
	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		MacroParameter parameter = (MacroParameter)o;
		return name().equals(parameter.name())&&Objects.equals(defaultValue(), parameter.defaultValue());
	}
	@Override public int hashCode(){
		return Objects.hash(name(), defaultValue());
	}

	@Override public void toString(StringBuilder stb, ToStringOption option, ToStringContext context){
		LiteralFormats.paramToString(stb, name());
		if(defaultValue()!=null)
			defaultValue().toString(stb.append('='), option, ToStringContext.NONE);
	}

	@Override public void toPrettyString(StringBuilder stb, int indents, ToStringOption option, ToStringContext context){
		toPrettyString(stb, indents, option, false);
	}

	public void toPrettyString(StringBuilder stb, int indents, ToStringOption option, boolean replaceDefaultValueWithStubs){
		LiteralFormats.paramToString(stb, name());
		if(defaultValue()!=null){
			if(replaceDefaultValueWithStubs){
				stb.append(" = /* default */");
			}else{
				defaultValue().toPrettyString(stb.append(" = "), indents+1, option, ToStringContext.NONE);
			}
		}
	}
}
