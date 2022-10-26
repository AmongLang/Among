package among.internals;

import among.internals.Token.TokenType;

/**
 * Mode for interpreting literal expressions.<br>
 * Literals can be read as separate expressions or one expression, depending on the context; snippet below shows the
 * example of such behavior.
 * <pre>
 * macro name=name(name=name): $name { name=name: ( name=name ) }
 * </pre>
 * <ul>
 *     <li>The first {@code 'name=name'} in definition name is read with {@link TokenizationMode#WORD},
 *     which produces literal of {@code 'name=name'}.</li>
 *     <li>The front half of the second {@code 'name=name'} in parameter is read with {@link TokenizationMode#PARAM_NAME},
 *     which produces literal of {@code 'name'}. The literal behind {@code =} is read with {@link TokenizationMode#VALUE},
 *     which produces literal of {@code 'name'}.</li>
 *     <li>The third {@code 'name=name'} is read with {@link TokenizationMode#KEY}, which produces {@code 'name=name'}.</li>
 *     <li>And, the last {@code 'name=name'} is read with {@link TokenizationMode#OPERATION},
 *     which separates {@code =} as binary operator and returns literal {@code 'name'}, operator {@code '='},
 *     and literal {@code 'name'} consecutively.</li>
 * </ul>
 *
 * @see Tokenizer#next(boolean, TokenizationMode)
 */
public enum TokenizationMode{
	/**
	 * Any encountered literals will produce {@link TokenType#ERROR ERROR}s.
	 */
	UNEXPECTED,
	/**
	 * Any encountered literals will produce {@link TokenType#WORD WORD}s.
	 */
	WORD,
	/**
	 * Any encountered literals will produce {@link TokenType#WORD WORD}s. Words with no escape sequences are instead
	 * emitted as {@link TokenType#PLAIN_WORD PLAIN_WORD}.
	 */
	PLAIN_WORD,
	/**
	 * Any encountered literals will produce {@link TokenType#KEY KEY}s.<br>
	 * '(', ')', '[' and ']' tokens are not parsed in this mode, as they are part of the key.
	 */
	KEY,
	/**
	 * Any encountered literals will produce {@link TokenType#PARAM_NAME}s.<br>
	 * Note that '=' token can only be parsed with this mode.
	 */
	PARAM_NAME,
	/**
	 * Any encountered literals will produce {@link TokenType#MACRO_NAME}s.
	 */
	MACRO_NAME,
	/**
	 * Any encountered literals will be parsed as values; see grammar for specific definition.
	 */
	VALUE,
	/**
	 * Makes the tokenizer search for operator/keyword definition while parsing literals.
	 * Operator/keyword tokens will only be parsed with this mod.
	 */
	OPERATION;

	/**
	 * @return Whether this mode emits colon(':') as its own token - if {@code false}, it indicates colons will be
	 * treated as a part of literal.
	 */
	public boolean emitsColon(){
		switch(this){
			case UNEXPECTED: case PLAIN_WORD: case KEY: case MACRO_NAME: return true;
			case WORD: case PARAM_NAME: case VALUE: case OPERATION: return false;
			default: throw new IllegalStateException("Unreachable");
		}
	}
}
