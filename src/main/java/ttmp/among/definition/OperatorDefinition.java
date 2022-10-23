package ttmp.among.definition;

import org.jetbrains.annotations.Nullable;
import ttmp.among.util.AmongUs;
import ttmp.among.util.PrettyFormatOption;
import ttmp.among.util.ToPrettyString;

import java.text.DecimalFormat;
import java.util.Objects;

/**
 * Operator definitions. Snippet below shows various operator definitions written in Among.
 * <pre>
 * operator ... as postfix
 * keyword is as binary
 * </pre>
 */
public final class OperatorDefinition implements ToPrettyString{
	private static final DecimalFormat FORMAT = new DecimalFormat("0.#####");

	private final String name;
	private final boolean isKeyword;
	private final OperatorType type;
	private final String alias;
	private final byte properties;
	private final double priority;

	/**
	 * Creates new operator definition.
	 *
	 * @param name      Name of the operator
	 * @param isKeyword Whether this defines keyword or operator
	 * @param type      Type of the operator
	 * @throws NullPointerException if {@code name == null} or {@code type == null}
	 * @see OperatorDefinition#OperatorDefinition(String, boolean, OperatorType, double)
	 */
	public OperatorDefinition(String name, boolean isKeyword, OperatorType type){
		this(name, isKeyword, type, Double.NaN);
	}
	/**
	 * Creates new operator definition.
	 *
	 * @param name      Name of the operator
	 * @param isKeyword Whether this defines keyword or operator
	 * @param type      Type of the operator
	 * @param priority  Priority of the operator; if {@code NaN} is supplied, it will be replaced with default priority.
	 * @throws NullPointerException if {@code name == null} or {@code type == null}
	 */
	public OperatorDefinition(String name, boolean isKeyword, OperatorType type, double priority){
		this(name, isKeyword, type, null, OperatorProperty.NONE, priority);
	}

	/**
	 * Creates new operator definition.
	 *
	 * @param name      Name of the operator
	 * @param isKeyword Whether this defines keyword or operator
	 * @param type      Type of the operator
	 * @param alias     Optional alias for the operator; if the value is present, resulting operation tree will have the alias as the name.
	 * @param priority  Priority of the operator; if {@code NaN} is supplied, it will be replaced with default priority.
	 * @throws NullPointerException if {@code name == null} or {@code type == null}
	 */
	public OperatorDefinition(String name, boolean isKeyword, OperatorType type, @Nullable String alias, byte properties, double priority){
		this.name = Objects.requireNonNull(name);
		this.isKeyword = isKeyword;
		this.type = Objects.requireNonNull(type);
		this.alias = alias;
		this.properties = OperatorProperty.normalize(type, properties);
		this.priority = Double.isNaN(priority) ? type.defaultPriority(properties) : priority;
	}

	public String name(){
		return name;
	}
	public boolean isKeyword(){
		return isKeyword;
	}
	public OperatorType type(){
		return type;
	}
	@Nullable public String alias(){
		return name;
	}
	public byte properties(){
		return properties;
	}
	public double priority(){
		return priority;
	}

	public boolean hasProperty(byte flag){
		return (properties&flag)==flag;
	}

	public String aliasOrName(){
		return alias!=null ? alias : name;
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		OperatorDefinition that = (OperatorDefinition)o;
		return isKeyword==that.isKeyword&&
				properties==that.properties&&
				Double.compare(that.priority, priority)==0&&
				name.equals(that.name)&&
				type==that.type&&
				Objects.equals(alias, that.alias);
	}
	@Override public int hashCode(){
		return Objects.hash(name, isKeyword, type, alias, properties, priority);
	}

	@Override public String toString(){
		StringBuilder stb = new StringBuilder().append(isKeyword ? "keyword " : "operator ");
		AmongUs.nameToString(stb, this.name, false);
		stb.append(" as ").append(OperatorProperty.typeToString(type, properties));
		if(Double.compare(priority, type.defaultPriority(properties))!=0)
			stb.append("(").append(FORMAT.format(priority)).append(")");
		if(alias!=null) AmongUs.primitiveToString(stb.append(":"), alias);
		return stb.toString();
	}
	@Override public String toPrettyString(int indents, PrettyFormatOption option){
		StringBuilder stb = new StringBuilder().append(isKeyword ? "keyword " : "operator ");
		AmongUs.nameToString(stb, this.name, false);
		stb.append(" as ").append(OperatorProperty.typeToString(type, properties));
		if(Double.compare(priority, type.defaultPriority(properties))!=0)
			stb.append("(").append(FORMAT.format(priority)).append(")");
		if(alias!=null) AmongUs.primitiveToPrettyString(stb.append(" : "), alias, indents, option);
		return stb.toString();
	}
}
