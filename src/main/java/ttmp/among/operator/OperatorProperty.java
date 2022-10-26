package ttmp.among.operator;

/**
 * Flags for operators/keywords.
 */
public interface OperatorProperty{
	/**
	 * Default property flags for operators.
	 */
	byte NONE = 0;

	/**
	 * Only applicable to binary operators. Marks the operator as right-associative.
	 */
	byte RIGHT_ASSOCIATIVE = 1;
	/**
	 * Only applicable to binary operators. Marks the operator as accessor: a special binary operator with additional
	 * parsing rules.
	 */
	byte ACCESSOR = 2;

	byte BINARY_ALL = RIGHT_ASSOCIATIVE|ACCESSOR;

	/**
	 * Trim out unnecessary values present in flags.
	 *
	 * @param type  Type of the operator
	 * @param flags Original flag
	 * @return Normalized flag
	 */
	static byte normalize(OperatorType type, byte flags){
		return (byte)(type==OperatorType.BINARY ? flags&BINARY_ALL : 0);
	}

	static String typeToString(OperatorType type, byte flags){
		if(type!=OperatorType.BINARY||(flags&BINARY_ALL)==0) return type.toString();
		return ((flags&RIGHT_ASSOCIATIVE)!=0 ? "right-associative binary" : "binary")
				+((flags&ACCESSOR)!=0 ? " accessor" : "");
	}
}
