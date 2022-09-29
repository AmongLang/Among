package ttmp.among.util;

import ttmp.among.AmongEngine;
import ttmp.among.compile.Source;

/**
 * Base type for objects providing pretty formatting. Most of Among related objects will yield re-compilable script which
 * will produce identical copy of them once read with {@link AmongEngine#read(Source)}.
 */
public interface ToPrettyString{
	/**
	 * Returns a string representation of this object, formatted in human-readable form. Default format option will be
	 * used.
	 *
	 * @return String representation of this object
	 * @see ToPrettyString#toPrettyString(int, PrettyFormatOption)
	 */
	default String toPrettyString(){
		return toPrettyString(0, PrettyFormatOption.DEFAULT);
	}

	/**
	 * Returns a string representation of this object, formatted in human-readable form. Default format option will be
	 * used.
	 *
	 * @param indents Number of indentations
	 * @return String representation of this object
	 * @see ToPrettyString#toPrettyString(int, PrettyFormatOption)
	 */
	default String toPrettyString(int indents){
		return toPrettyString(indents, PrettyFormatOption.DEFAULT);
	}

	/**
	 * Returns a string representation of this object, formatted in human-readable form. Default format option will be
	 * used.
	 *
	 * @param option  Option to use
	 * @return String representation of this object
	 * @see ToPrettyString#toPrettyString(int, PrettyFormatOption)
	 */
	default String toPrettyString(PrettyFormatOption option){
		return toPrettyString(0, option);
	}

	/**
	 * Returns a string representation of this object, formatted in human-readable form.
	 *
	 * @param indents Number of indentations
	 * @param option  Option to use
	 * @return String representation of this object
	 */
	String toPrettyString(int indents, PrettyFormatOption option);
}
