package ttmp.among.operator;

import ttmp.among.util.AmongUs;

import java.text.DecimalFormat;
import java.util.Objects;

/**
 * Operator definitions. Snippet below shows various operator definitions written in Among.
 * <pre>
 * operator ... as postfix
 * keyword is as binary
 * </pre>
 */
public final class OperatorDefinition{
	private static final DecimalFormat FORMAT = new DecimalFormat("0.#####");

	private final String name;
	private final boolean isKeyword;
	private final OperatorType type;
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
		this.name = Objects.requireNonNull(name);
		this.isKeyword = isKeyword;
		this.type = Objects.requireNonNull(type);
		this.priority = Double.isNaN(priority) ? type.defaultPriority() : priority;
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
	public double priority(){
		return priority;
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		OperatorDefinition that = (OperatorDefinition)o;
		return isKeyword()==that.isKeyword()&&
				Double.compare(that.priority, priority)==0&&
				name.equals(that.name)&&
				type==that.type;
	}
	@Override public int hashCode(){
		return Objects.hash(name, isKeyword(), type, priority);
	}

	@Override public String toString(){
		StringBuilder stb = new StringBuilder().append(isKeyword ? "keyword " : "operator ");
		AmongUs.nameToString(stb, this.name);
		stb.append(" as ")
				.append(type==OperatorType.BINARY ? "binary" : type==OperatorType.POSTFIX ? "postfix" : "prefix");
		if(Double.compare(priority, type.defaultPriority())!=0)
			stb.append(" : ").append(FORMAT.format(priority));
		return stb.toString();
	}
}
