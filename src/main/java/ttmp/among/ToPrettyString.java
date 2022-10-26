package ttmp.among;

import ttmp.among.compile.Source;

/**
 * Base type for objects providing pretty formatting. Most of Among related objects will yield re-compilable script which
 * will produce identical copy of them once read with {@link AmongEngine#read(Source)}.<br>
 * All instances implementing this interface is expected to override {@link Object#toString()} to call
 * {@link ToPrettyString#toString(PrettifyOption)} with option of {@link PrettifyOption#DEFAULT}.
 */
public interface ToPrettyString{
	/**
	 * Returns a string representation of this object, formatted in most compact form.
	 *
	 * @param option Option to use
	 * @return String representation of this object
	 */
	default String toString(PrettifyOption option){
		return toString(option, PrettifyContext.NONE);
	}
	/**
	 * Returns a string representation of this object, formatted in most compact form.
	 *
	 * @param option  Option to use
	 * @param context Context of the formatting
	 * @return String representation of this object
	 */
	default String toString(PrettifyOption option, PrettifyContext context){
		StringBuilder stb = new StringBuilder();
		toString(stb, option, context);
		return stb.toString();
	}
	/**
	 * Inserts string representation of this object to {@code stb}, formatted in most compact form.
	 *
	 * @param stb     String builder to be appended
	 * @param option  Option to use
	 * @param context Context of the formatting
	 */
	void toString(StringBuilder stb, PrettifyOption option, PrettifyContext context);

	/**
	 * Returns a string representation of this object, formatted in human-readable form. Default format option will be
	 * used.
	 *
	 * @return String representation of this object
	 * @see ToPrettyString#toPrettyString(int, PrettifyOption, PrettifyContext)
	 */
	default String toPrettyString(){
		return toPrettyString(0, PrettifyOption.DEFAULT);
	}

	/**
	 * Returns a string representation of this object, formatted in human-readable form. Default format option will be
	 * used.
	 *
	 * @param indents Number of indentations
	 * @return String representation of this object
	 * @see ToPrettyString#toPrettyString(int, PrettifyOption, PrettifyContext)
	 */
	default String toPrettyString(int indents){
		return toPrettyString(indents, PrettifyOption.DEFAULT);
	}

	/**
	 * Returns a string representation of this object, formatted in human-readable form. Default format option will be
	 * used.
	 *
	 * @param option Option to use
	 * @return String representation of this object
	 * @see ToPrettyString#toPrettyString(int, PrettifyOption, PrettifyContext)
	 */
	default String toPrettyString(PrettifyOption option){
		return toPrettyString(0, option);
	}

	/**
	 * Returns a string representation of this object, formatted in human-readable form.
	 *
	 * @param indents Number of indentations
	 * @param option  Option to use
	 * @return String representation of this object
	 */
	default String toPrettyString(int indents, PrettifyOption option){
		return toPrettyString(indents, option, PrettifyContext.NONE);
	}

	/**
	 * Returns a string representation of this object, formatted in human-readable form.
	 *
	 * @param indents Number of indentations
	 * @param option  Option to use
	 * @param context Context of the formatting
	 * @return String representation of this object
	 */
	default String toPrettyString(int indents, PrettifyOption option, PrettifyContext context){
		StringBuilder stb = new StringBuilder();
		toPrettyString(stb, indents, option, context);
		return stb.toString();
	}

	/**
	 * Inserts string representation of this object to {@code stb}, formatted in human-readable form.
	 *
	 * @param stb     String builder to be appended
	 * @param indents Number of indentations
	 * @param option  Option to use
	 * @param context Context of the formatting
	 */
	void toPrettyString(StringBuilder stb, int indents, PrettifyOption option, PrettifyContext context);

	/**
	 * 'Base implementation' of {@link ToPrettyString}. (which means just overriding {@link Object#toString()})
	 */
	abstract class Base implements ToPrettyString{
		@Override public final String toString(){
			return toString(PrettifyOption.DEFAULT);
		}
	}
}
