package ttmp.among.internals;

import org.jetbrains.annotations.Nullable;
import ttmp.among.exception.Sussy;
import ttmp.among.operator.OperatorDefinition;

import java.util.Objects;

/**
 * Individual unit of source code, produced by {@link AmongTokenizer}.
 */
public final class AmongToken{
	public final TokenType type;
	public final int start;
	public final @Nullable String literal;

	public AmongToken(TokenType type, int start){
		this(type, start, null);
	}
	public AmongToken(TokenType type, int start, @Nullable String literal){
		this.type = type;
		this.start = start;
		this.literal = literal;
	}

	public boolean isLiteral(){
		return literal!=null;
	}

	public String expectLiteral(){
		if(literal==null) throw new Sussy("Expected literal");
		return literal;
	}

	public String keywordOrEmpty(){
		return is(TokenType.PLAIN_WORD)&&literal!=null ? literal : "";
	}

	public boolean is(TokenType type){
		return this.type==type;
	}
	public boolean is(TokenType type, @Nullable String literal){
		return is(type)&&Objects.equals(literal, this.literal);
	}

	public boolean isSimpleLiteral(){
		return !is(TokenType.QUOTED_PRIMITIVE)&&isLiteral();
	}

	/**
	 * Tries to parse literal value to double.
	 *
	 * @return Double value parsed from literal, or {@code NaN} if it failed
	 */
	public double asNumber(){ // TODO maybe I shouldn't just plug in java parseDouble() :P
		if(!is(TokenType.VALUE)) return Double.NaN;
		try{
			return Double.parseDouble(expectLiteral());
		}catch(NumberFormatException ex){
			return Double.NaN;
		}
	}

	public boolean isOperatorOrKeyword(){
		return is(TokenType.OPERATOR)||is(TokenType.KEYWORD);
	}

	public boolean is(OperatorDefinition operator){
		return isOperatorOrKeyword()&&expectLiteral().equals(operator.name());
	}

	@Override public String toString(){
		return type+":"+start+(literal!=null ? "("+literal+")" : "");
	}

	public enum TokenType{
		/** End of file */ EOF,
		/** Newline */ BR,
		/** ( */ L_PAREN,
		/** ) */ R_PAREN,
		/** { */ L_BRACE,
		/** } */ R_BRACE,
		/** [ */ L_BRACKET,
		/** ] */ R_BRACKET,
		/** = */ EQ,
		/** , */ COMMA,
		/** : */ COLON,
		// Literal shits
		/**
		 * One-letter literal.
		 */
		WORD,
		/**
		 * One-letter literal with no escape sequences.
		 */
		PLAIN_WORD,
		KEY, PARAM_NAME, MACRO_NAME, QUOTED_PRIMITIVE, VALUE, OPERATOR, KEYWORD, NUMBER,
		/** Generic error token; only emitted on special occasions */ ERROR;

		public String friendlyName(){
			switch(this){
				case EOF: return "end of file";
				case BR: return "line break";
				case L_PAREN: return "'('";
				case R_PAREN: return "')'";
				case L_BRACE: return "'{'";
				case R_BRACE: return "'}'";
				case L_BRACKET: return "'['";
				case R_BRACKET: return "']'";
				case EQ: return "'='";
				case COMMA: return "','";
				case COLON: return "':'";
				case PLAIN_WORD: return "plain word";
				case WORD: return "word";
				case KEY: return "key";
				case PARAM_NAME: return "parameter name";
				case MACRO_NAME: return "macro name";
				case QUOTED_PRIMITIVE: return "quoted primitive";
				case VALUE: return "value";
				case OPERATOR: return "operator";
				case KEYWORD: return "keyword";
				case NUMBER: return "number";
				case ERROR: return "error";
				default: throw new IllegalStateException("Unreachable");
			}
		}
	}
}
