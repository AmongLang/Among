package ttmp.among;

import java.util.Objects;

/**
 * @see ToStringOption#variant()
 */
public final class ToStringOptionBuilder{
	private String indent;
	private int compactObjectSize;
	private int compactListSize;
	private boolean jsonCompatibility;

	public ToStringOptionBuilder(ToStringOption original){
		this.indent = original.indent;
		this.compactObjectSize = original.compactObjectSize;
		this.compactListSize = original.compactListSize;
		this.jsonCompatibility = original.jsonCompatibility;
	}

	public ToStringOptionBuilder indent(String indent){
		this.indent = Objects.requireNonNull(indent);
		return this;
	}

	public ToStringOptionBuilder compactObjectSize(int compactObjectSize){
		this.compactObjectSize = compactObjectSize;
		return this;
	}

	public ToStringOptionBuilder compactListSize(int compactListSize){
		this.compactListSize = compactListSize;
		return this;
	}

	public ToStringOptionBuilder jsonCompatibility(boolean jsonCompatibility){
		this.jsonCompatibility = jsonCompatibility;
		return this;
	}

	public ToStringOptionBuilder jsonCompatible(){
		this.jsonCompatibility = true;
		return this;
	}

	public ToStringOption build(){
		return new ToStringOption(indent, compactObjectSize, compactListSize, jsonCompatibility);
	}
}
