package among.macro;

import among.internals.LiteralFormats;
import among.obj.Among;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Signature of the macro - name and type.
 *
 * @see MacroDefinition
 */
public final class MacroSignature implements Comparable<MacroSignature>{
	/**
	 * Create a new instance matching the Among value.
	 *
	 * @param among Among value
	 * @return A new macro signature matching the value
	 */
	public static MacroSignature of(Among among){
		if(among.isPrimitive()) return new MacroSignature(among.asPrimitive().getValue(), MacroType.CONST);
		return new MacroSignature(among.asNameable().getName(),
				among.isObj() ? MacroType.OBJECT :
						among.asList().isOperation() ? MacroType.OPERATION :
								MacroType.LIST);
	}

	private final String name;
	private final MacroType type;

	public MacroSignature(String name, MacroType type){
		this.name = Objects.requireNonNull(name);
		this.type = Objects.requireNonNull(type);
	}

	public String name(){
		return name;
	}
	public MacroType type(){
		return type;
	}

	@Override public int compareTo(@NotNull MacroSignature o){
		int c = name.compareTo(o.name);
		if(c!=0) return c;
		return type.compareTo(o.type);
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		MacroSignature that = (MacroSignature)o;
		return name.equals(that.name)&&type==that.type;
	}
	@Override public int hashCode(){
		return Objects.hash(name, type);
	}

	@Override public String toString(){
		StringBuilder stb = new StringBuilder();
		if(LiteralFormats.isSimpleMacroName(name())) LiteralFormats.simpleMacroNameToString(stb, name());
		else LiteralFormats.primitiveToString(stb, name());
		switch(type){
			case OBJECT: case OBJECT_FN:
				stb.append("{}");
				break;
			case LIST: case LIST_FN:
				stb.append("[]");
				break;
			case OPERATION: case OPERATION_FN:
				stb.append("()");
				break;
			case CONST: case ACCESS: break;
			default: throw new IllegalStateException("unreachable");
		}
		return stb.toString();
	}
}
