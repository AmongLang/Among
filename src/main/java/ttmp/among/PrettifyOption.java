package ttmp.among;

import java.util.Objects;

/**
 * Formatting option used in {@link ToPrettyString}.
 */
public final class PrettifyOption{
	public static final PrettifyOption DEFAULT = new PrettifyOption("  ", 2, 3, false);

	/**
	 * Indentation to be used.
	 */
	public final String indent;
	/**
	 * Inclusive maximum size for objects to be formatted 'compact' - the object will be written in one line, without
	 * excessive whitespaces or line breaks. Compact formatting does not affect child elements.
	 */
	public final int compactObjectSize;
	/**
	 * Inclusive maximum size for lists to be formatted 'compact' - the list will be written in one line, without
	 * excessive whitespaces or line breaks. Compact formatting does not affect child elements.
	 */
	public final int compactListSize;
	/**
	 * If set to true, most of the formatting will produce JSON-compatible scripts. Note that some features, like
	 * macro definitions, operator definitions, multiple roots in a script, etc. cannot be read with JSON, as JSON
	 * does not include such feature.
	 */
	public final boolean jsonCompatibility;

	public PrettifyOption(String indent, int compactObjectSize, int compactListSize, boolean jsonCompatibility){
		this.indent = Objects.requireNonNull(indent);
		this.compactObjectSize = compactObjectSize;
		this.compactListSize = compactListSize;
		this.jsonCompatibility = jsonCompatibility;
	}

	@Override public boolean equals(Object o){
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;
		PrettifyOption that = (PrettifyOption)o;
		return compactObjectSize==that.compactObjectSize&&
				compactListSize==that.compactListSize&&
				jsonCompatibility==that.jsonCompatibility&&
				indent.equals(that.indent);
	}
	@Override public int hashCode(){
		return Objects.hash(indent, compactObjectSize, compactListSize, jsonCompatibility);
	}

	@Override public String toString(){
		return "PrettifyOption{"+
				"indent='"+indent+'\''+
				", compactObjectSize="+compactObjectSize+
				", compactListSize="+compactListSize+
				", jsonCompatibility="+jsonCompatibility+
				'}';
	}
}
