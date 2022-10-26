package ttmp.among.obj;

import ttmp.among.AmongWalker;
import ttmp.among.exception.Sussy;
import ttmp.among.internals.LiteralFormats;
import ttmp.among.PrettifyContext;
import ttmp.among.PrettifyOption;
import ttmp.among.NodePath;

import java.util.Objects;

/**
 * {@link Among} node representing a string value. Snippet below shows a primitive written in Among.
 * <pre>
 * "Hello!"
 * </pre>
 */
public class AmongPrimitive extends Among{
	private String value;

	AmongPrimitive(){
		this("");
	}
	AmongPrimitive(String value){
		this.value = Objects.requireNonNull(value);
	}

	public String getValue(){
		return value;
	}
	public void setValue(String value){
		this.value = Objects.requireNonNull(value);
	}

	/**
	 * @return Value of this primitive parsed into integer, using {@link Integer#parseInt(String)}
	 * @throws NumberFormatException If the value is not a valid int value
	 */
	public int getIntValue(){
		return Integer.parseInt(getValue());
	}
	/**
	 * @return Value of this primitive parsed into long, using {@link Long#parseLong(String)}
	 * @throws NumberFormatException If the value is not a valid long value
	 */
	public long getLongValue(){
		return Long.parseLong(getValue());
	}
	/**
	 * @return Value of this primitive parsed into float, using {@link Float#parseFloat(String)}
	 * @throws NumberFormatException If the value is not a valid float value
	 */
	public float getFloatValue(){
		return Float.parseFloat(getValue());
	}
	/**
	 * @return Value of this primitive parsed into double, using {@link Double#parseDouble(String)}
	 * @throws NumberFormatException If the value is not a valid double value
	 */
	public double getDoubleValue(){
		return Double.parseDouble(getValue());
	}
	/**
	 * @return Value of this primitive parsed into boolean. This method returns {@code true} or
	 * {@code false} if the value is equal to, ignoring case, {@code "true"} and {@code "false"}
	 * respectively.
	 * @throws Sussy If the value is not a valid boolean value
	 */
	public boolean getBoolValue(){
		if(getValue().equalsIgnoreCase("true")) return true;
		else if(getValue().equalsIgnoreCase("false")) return false;
		else throw new Sussy("Value '"+getValue()+"' cannot be parsed to boolean");
	}

	/**
	 * @return Value of this primitive parsed into integer, using {@link Integer#parseInt(String)}.
	 * If the operation fails, {@code fallback} will be returned.
	 */
	public int getIntValue(int fallback){
		try{
			return Integer.parseInt(getValue());
		}catch(NumberFormatException ex){
			return fallback;
		}
	}
	/**
	 * @return Value of this primitive parsed into long, using {@link Long#parseLong(String)}.
	 * If the operation fails, {@code fallback} will be returned.
	 */
	public long getLongValue(long fallback){
		try{
			return Long.parseLong(getValue());
		}catch(NumberFormatException ex){
			return fallback;
		}
	}
	/**
	 * @return Value of this primitive parsed into float, using {@link Float#parseFloat(String)}.
	 * If the operation fails, {@code fallback} will be returned.
	 */
	public float getFloatValue(float fallback){
		try{
			return Float.parseFloat(getValue());
		}catch(NumberFormatException ex){
			return fallback;
		}
	}
	/**
	 * @return Value of this primitive parsed into double, using {@link Double#parseDouble(String)}.
	 * If the operation fails, {@code fallback} will be returned.
	 */
	public double getDoubleValue(double fallback){
		try{
			return Double.parseDouble(getValue());
		}catch(NumberFormatException ex){
			return fallback;
		}
	}
	/**
	 * @return Value of this primitive parsed into boolean. This method returns {@code true} or
	 * {@code false} if the value is equal to, ignoring case, {@code "true"} and {@code "false"}
	 * respectively. If the operation fails, {@code fallback} will be returned.
	 */
	public boolean getBoolValue(boolean fallback){
		if(getValue().equalsIgnoreCase("true")) return true;
		else if(getValue().equalsIgnoreCase("false")) return false;
		else return fallback;
	}

	@Override public AmongPrimitive asPrimitive(){
		return this;
	}
	@Override public boolean isPrimitive(){
		return true;
	}

	@Override public void walk(AmongWalker visitor, NodePath path){
		visitor.walk(this, path);
	}

	@Override public AmongPrimitive copy(){
		return new AmongPrimitive(this.value);
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		AmongPrimitive that = (AmongPrimitive)o;
		return Objects.equals(value, that.value);
	}
	@Override public int hashCode(){
		return Objects.hash(value);
	}

	@Override public void toString(StringBuilder stb, PrettifyOption option, PrettifyContext context){
		boolean useQuote = context!=PrettifyContext.NONE||!LiteralFormats.isSimpleValue(getValue())||option.jsonCompatibility;
		if(useQuote) LiteralFormats.primitiveToString(stb, getValue());
		else LiteralFormats.simpleValueToString(stb, getValue());
	}

	@Override public void toPrettyString(StringBuilder stb, int indents, PrettifyOption option, PrettifyContext context){
		boolean useQuote = context!=PrettifyContext.NONE||!LiteralFormats.isSimpleValue(getValue())||option.jsonCompatibility;
		if(useQuote) LiteralFormats.primitiveToPrettyString(stb, getValue(), indents, option);
		else LiteralFormats.simpleValueToString(stb, getValue());
	}
}
