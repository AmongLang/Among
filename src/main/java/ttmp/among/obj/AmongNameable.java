package ttmp.among.obj;

import org.jetbrains.annotations.Nullable;
import ttmp.among.format.AmongLiteralFormatting;
import ttmp.among.format.PrettifyContext;
import ttmp.among.format.PrettifyOption;

/**
 * Base class for all 'nameable' objects; i.e.
 * <ul>
 *     <li>{@link AmongObject object},</li>
 *     <li>{@link AmongList list},</li>
 *     <li>and operation, which is just an alternative syntax for creating lists.</li>
 * </ul>
 * <p>
 * Object defined with unspecified name is equivalent to the ones defined with empty name.
 *
 * @see AmongObject
 * @see AmongList
 */
public abstract class AmongNameable extends Among{
	private String name;

	AmongNameable(){
		this(null);
	}
	AmongNameable(@Nullable String name){
		this.name = name==null ? "" : name;
	}

	/**
	 * Returns the name of this object. If unspecified, it will return empty string.
	 *
	 * @return The name of this object
	 */
	public String getName(){
		return name;
	}
	/**
	 * Sets the name of this object. When {@code null} or empty string is provided, it 'removes' the name.
	 *
	 * @param name The name to be set
	 */
	public void setName(@Nullable String name){
		this.name = name==null ? "" : name;
	}
	/**
	 * Return whether this object has a name (that is, result of {@link AmongNameable#getName()} being not empty)
	 *
	 * @return Whether this object has a name
	 */
	public boolean hasName(){
		return !name.isEmpty();
	}

	@Override public AmongNameable asNameable(){
		return this;
	}
	@Override public boolean isNameable(){
		return true;
	}

	@Override public abstract AmongNameable copy();

	protected void nameToString(StringBuilder stb, PrettifyOption option, PrettifyContext context){
		if(hasName()){
			if(context!=PrettifyContext.OPERATION&&AmongLiteralFormatting.isSimpleValue(getName()))
				AmongLiteralFormatting.simpleValueToString(stb, getName());
			else AmongLiteralFormatting.primitiveToString(stb, getName());
		}
	}
	protected void nameToPrettyString(StringBuilder stb, int indents, PrettifyOption option, PrettifyContext context){
		if(hasName()){
			if(context!=PrettifyContext.OPERATION&&AmongLiteralFormatting.isSimpleValue(getName()))
				AmongLiteralFormatting.simpleValueToString(stb, getName());
			else AmongLiteralFormatting.primitiveToPrettyString(stb, getName(), indents+1, option);
		}
	}
}
