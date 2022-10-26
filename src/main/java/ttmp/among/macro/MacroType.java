package ttmp.among.macro;

/**
 * Types of macro. Snippet below shows macros with each type written in Among.
 * <pre>
 * macro macro : "Hello!"  // CONST
 * macro macro{} : "Hello!"  // OBJECT
 * macro macro[] : "Hello!"  // LIST
 * macro macro() : "Hello!"  // OPERATION
 * </pre>
 */
public enum MacroType{
	/**
	 * Macro defined without any parameter blocks. Not to be confused with {@link MacroDefinition#isConstant()}.
	 */
	CONST,
	/**
	 * Macro defined with object ('{}') as parameter blocks.
	 */
	OBJECT,
	/**
	 * Macro defined with list ('[]') as parameter blocks.
	 */
	LIST,
	/**
	 * Macro defined with operation ('()') as parameter blocks.
	 */
	OPERATION,
	/**
	 * Field access.
	 */
	ACCESS,
	/**
	 * Object method invocation.
	 */
	OBJECT_FN,
	/**
	 * List method invocation.
	 */
	LIST_FN,
	/**
	 * Operation method invocation.
	 */
	OPERATION_FN;

	public boolean isFunctionMacro(){
		return this==ACCESS||this==OBJECT_FN||this==LIST_FN||this==OPERATION_FN;
	}

	public String friendlyName(){
		switch(this){
			case CONST: return "constant";
			case OBJECT: return "object";
			case LIST: return "list";
			case OPERATION: return "operation";
			case ACCESS: return "access";
			case OBJECT_FN: return "object function";
			case LIST_FN: return "list function";
			case OPERATION_FN: return "operation function";
			default: throw new IllegalStateException("Unreachable");
		}
	}
}
