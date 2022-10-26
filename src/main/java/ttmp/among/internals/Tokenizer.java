package ttmp.among.internals;

import org.jetbrains.annotations.Nullable;
import ttmp.among.ErrorHandling;
import ttmp.among.ReportType;
import ttmp.among.Source;
import ttmp.among.internals.Token.TokenType;
import ttmp.among.operator.OperatorRegistry.NameGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static ttmp.among.Source.EOF;

/**
 * Object responsible for converting raw source strings into list of {@link Token}s.<br>
 * The tokenization process is mode dependant - same input might produce different result depending on the mode.
 * To compensate for the possible ambiguity of the compilation, a rudimentary 'setback' functionality is included,
 * which act as a kind of lookahead. (ok i know these ramblings sounded absolutely fucking terrible but its my child ok
 * dont be a dick and move on)
 *
 * @see Token
 * @see TokenizationMode
 */
public final class Tokenizer{
	private final Source source;
	private final Parser parser;

	private int srcIndex;

	private final List<Token> tokens = new ArrayList<>();
	private int tokenIndex;
	private int lastSrcIndex;
	private int lastTokensLeft;

	public Tokenizer(Source source, Parser parser){
		this.source = source;
		this.parser = parser;
	}

	public Source source(){
		return source;
	}

	/**
	 * Advances to the next token. New token is read if necessary.
	 *
	 * @param skipLineBreak If {@code true}, line break tokens will be skipped.
	 * @param mode          Mode for interpreting literal expressions.
	 * @return The next token
	 */
	public Token next(boolean skipLineBreak, TokenizationMode mode){
		while(true){
			Token token = advance(mode);
			if(!skipLineBreak||token.type!=TokenType.BR) return token;
		}
	}

	/**
	 * @return Last token read and returned with {@link Tokenizer#next(boolean, TokenizationMode) next()},
	 * or {@code null} if there isn't one (because either {@link Tokenizer#reset(boolean) reset()} was called or
	 * nothing was read yet)
	 */
	@Nullable public Token lastToken(){
		return lastToken;
	}

	@Nullable private Token lastToken;

	/**
	 * Advances token index; Returns EOF if it's already at the end.
	 */
	private Token advance(TokenizationMode mode){
		if(tokenIndex>=tokens.size()){
			read(mode);
			if(tokenIndex>=tokens.size())
				return lastToken = new Token(TokenType.EOF, srcIndex);
		}
		return lastToken = tokens.get(tokenIndex++);
	}

	/**
	 * Discards all tokens currently read at this point. Current point becomes starting index.
	 */
	public void discard(){
		if(tokenIndex>0) tokens.subList(0, tokenIndex).clear();
		tokenIndex = 0;
		lastSrcIndex = srcIndex;
		lastTokensLeft = tokens.size();
	}

	/**
	 * Resets current index to starting index.
	 */
	public void reset(){
		reset(false);
	}

	/**
	 * Resets current index to starting index.
	 *
	 * @param discardTokens If {@code true}, discards all tokens read after starting index. If {@code false}, tokens
	 *                      will be retained.<br>
	 *                      This option is used to interpret same portion of source with two different modes.
	 */
	public void reset(boolean discardTokens){
		if(discardTokens){
			tokens.subList(lastTokensLeft, tokens.size()).clear();
			srcIndex = lastSrcIndex;
		}
		tokenIndex = 0;
		lastToken = null;
	}

	private void read(TokenizationMode mode){
		while(true){
			int idx = srcIndex;
			switch(nextCodePoint()){
				case EOF: return;
				case ' ': case '\t': continue;
				case '\n':
					tokens.add(new Token(TokenType.BR, idx));
					return;
				case '(':
					if(mode==TokenizationMode.KEY) break;
					tokens.add(new Token(TokenType.L_PAREN, idx));
					return;
				case ')':
					if(mode==TokenizationMode.KEY) break;
					tokens.add(new Token(TokenType.R_PAREN, idx));
					return;
				case '{':
					tokens.add(new Token(TokenType.L_BRACE, idx));
					return;
				case '}':
					tokens.add(new Token(TokenType.R_BRACE, idx));
					return;
				case '[':
					if(mode==TokenizationMode.KEY) break;
					tokens.add(new Token(TokenType.L_BRACKET, idx));
					return;
				case ']':
					if(mode==TokenizationMode.KEY) break;
					tokens.add(new Token(TokenType.R_BRACKET, idx));
					return;
				case ':':
					if(mode.emitsColon()){
						tokens.add(new Token(TokenType.COLON, idx));
						return;
					}else break;
				case ',':
					tokens.add(new Token(TokenType.COMMA, idx));
					return;
				case '\'':
					tokens.add(new Token(TokenType.QUOTED_PRIMITIVE, idx, primitive('\'')));
					return;
				case '"':
					tokens.add(new Token(TokenType.QUOTED_PRIMITIVE, idx, primitive('"')));
					return;
				case '=':
					if(mode==TokenizationMode.PARAM_NAME){
						tokens.add(new Token(TokenType.EQ, idx));
						return;
					}
			}
			if(mode==TokenizationMode.UNEXPECTED){
				tokens.add(new Token(TokenType.ERROR, idx));
				return;
			}
			srcIndex = idx;
			switch(mode){
				case PLAIN_WORD: tokens.add(word(true, false)); return;
				case WORD: tokens.add(word(false, false)); return;
				case KEY: tokens.add(multipleWords(true, false)); return;
				case PARAM_NAME: tokens.add(word(false, true)); return;
				case MACRO_NAME: tokens.add(multipleWords(false, true)); return;
				case VALUE: tokens.add(multipleWords(false, false)); return;
				case OPERATION: operation(); return;
			}
		}
	}

	private String primitive(int closure){
		StringBuilder stb = new StringBuilder();
		while(true){
			int c = nextCodePoint(true);
			switch(c){
				case EOF:
					parser.reportError("Unterminated primitive", srcIndex);
					return stb.toString();
				case '\\':
					stb.appendCodePoint(backslash());
					break;
				case '\n':{
					stb.append('\n');
					int prev = srcIndex;
					L2:
					while(true){ // search for |
						switch(nextCodePoint()){
							case ' ': case '\t': continue;
							case '|': break L2; // | found at the start of new line, continue from here
							default:
								// no | found
								srcIndex = prev;
								break L2;
						}
					}
					break;
				}
				default:
					if(c==closure) return stb.toString();
					else stb.appendCodePoint(c);
			}
		}
	}

	private Token word(boolean plain, boolean paramName){
		boolean isPlain = plain;
		StringBuilder stb = new StringBuilder();
		int start = srcIndex;
		int prev;
		L:
		while(true){
			prev = srcIndex;
			int c = nextCodePoint();
			switch(c){
				case '\\':
					isPlain = false;
					stb.appendCodePoint(backslash());
					continue;
				case '=':
					if(paramName) break L;
					else break;
				case ':':
					if(plain) break L;
					break;
				case EOF: case ' ': case '\t': case '\n': case ',':
				case '{': case '}': case '[': case ']': case '(': case ')':
					break L;
			}
			stb.appendCodePoint(c);
		}
		srcIndex = prev;
		return new Token(
				paramName ? TokenType.PARAM_NAME : isPlain ? TokenType.PLAIN_WORD : TokenType.WORD,
				start, stb.toString());
	}

	private Token multipleWords(boolean key, boolean macroName){
		StringBuilder stb = new StringBuilder();
		int start = srcIndex;
		int lastNonWhitespaceSeen = srcIndex;
		int prev;
		L:
		while(true){
			prev = srcIndex;
			switch(nextCodePoint()){
				case ' ': case '\t': continue;
				case ':':
					if(key||macroName) break L;
					else break;
				case '[': case ']': case '(': case ')':
					if(key) break;
				case EOF: case '\n': case ',': case '{': case '}':
					break L;
			}
			srcIndex = lastNonWhitespaceSeen;
			while(srcIndex<=prev) stb.appendCodePoint(nextLiteralChar());
			lastNonWhitespaceSeen = srcIndex;
		}
		srcIndex = prev;
		return new Token(
				key ? TokenType.KEY : macroName ? TokenType.MACRO_NAME : TokenType.VALUE,
				start, stb.toString());
	}

	private void operation(){
		int start = srcIndex;
		NameGroup keyword = match(true);
		StringBuilder stb = new StringBuilder();
		while(true){
			int prev = srcIndex;
			int c = nextCodePoint();
			switch(c){
				case ' ': case '\t': case '\n': case ':': case ',':
				case '{': case '}': case '[': case ']': case '(': case ')':
					srcIndex = prev;
				case EOF:
					addOperationTokens(keyword, start, stb);
					return;
			}
			srcIndex = prev;
			NameGroup operator = match(false);
			if(operator!=null){
				addOperationTokens(keyword, start, stb);
				tokens.add(new Token(TokenType.OPERATOR, prev, operator.name()));
				return;
			}
			if(keyword!=null){
				keyword = null;
				srcIndex = start;
			}else if(prev==start){ // first character
				if(number()) return;
			}
			stb.appendCodePoint(nextLiteralChar());
		}
	}

	private void addOperationTokens(@Nullable NameGroup keyword, int start,
	                                StringBuilder stb){
		if(keyword!=null) tokens.add(new Token(TokenType.KEYWORD, start, keyword.name()));
		else if(stb.length()>0) tokens.add(new Token(TokenType.WORD, start, stb.toString()));
	}

	@Nullable private NameGroup match(boolean keyword){
		int prev = srcIndex;
		Set<NameGroup> set = keyword ?
				parser.importRoot().operators().getKeywords(nextLiteralChar()) :
				parser.importRoot().operators().getOperators(nextLiteralChar());
		srcIndex = prev;
		if(!set.isEmpty())
			for(NameGroup o : set)
				if(matches(o)) return o;
		return null;
	}

	private boolean number(){
		int start = srcIndex;
		int numberStart = start;
		switch(nextCodePoint()){
			case '+': numberStart = srcIndex;
			case '-': switch(nextCodePoint()){
				case '0': case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
					break;
				default: srcIndex = start; return false;
			}
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
				break;
			default: srcIndex = start; return false;
		}
		srcIndex = numberStart;
		while(true){
			int prev = srcIndex;
			int c = nextCodePoint();
			switch(c){
				case '0': case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
					continue;
				case ' ': case '\t': case '\n': case ':': case ',':
				case '{': case '}': case '[': case ']': case '(': case ')':
					srcIndex = prev;
				case EOF:
					addNumber(numberStart, prev);
					return true;
				case '.':{
					boolean first = true;
					while(true){
						int prev2 = srcIndex;
						c = nextCodePoint();
						switch(c){
							case '0': case '1': case '2': case '3': case '4':
							case '5': case '6': case '7': case '8': case '9':
								if(first) first = false;
								continue;
							case ' ': case '\t': case '\n': case ':': case ',':
							case '{': case '}': case '[': case ']': case '(': case ')':
							case EOF:
								if(!first){
									srcIndex = prev2;
									addNumber(numberStart, prev2);
									return true;
								}
								break;
							default:
								if(!first){
									srcIndex = prev2;
									NameGroup operator = match(false);
									if(operator!=null){
										addNumber(numberStart, prev2);
										tokens.add(new Token(TokenType.OPERATOR, prev2, operator.name()));
										return true;
									}
								}
						}
						break;
					}
				}
				default:
					srcIndex = prev;
					NameGroup operator = match(false);
					if(operator!=null){
						addNumber(numberStart, prev);
						tokens.add(new Token(TokenType.OPERATOR, prev, operator.name()));
						return true;
					}else{
						srcIndex = start;
						return false;
					}
			}
		}
	}

	private void addNumber(int numberStartInclusive, int numberEndExclusive){
		int cache = srcIndex;
		srcIndex = numberStartInclusive;
		StringBuilder stb = new StringBuilder();
		while(srcIndex<numberEndExclusive)
			stb.appendCodePoint(nextLiteralChar());
		tokens.add(new Token(TokenType.NUMBER, numberStartInclusive, stb.toString()));
		srcIndex = cache;
	}

	private boolean matches(NameGroup operator){
		int prev = srcIndex;
		for(int i = 0; i<operator.codePointLength(); i++){
			if(operator.codePointAt(i)!=nextLiteralChar()){
				srcIndex = prev;
				return false;
			}
		}
		return true;
	}

	private int nextCodePoint(){
		return nextCodePoint(false);
	}
	private int nextCodePoint(boolean ignoreComment){
		while(true){
			if(!source.isInBounds(srcIndex)) return EOF;
			int c = source.codePointAt(srcIndex++);
			switch(c){
				case '\\': switch(source.codePointAt(srcIndex)){
					case '\r': if(source.codePointAt(srcIndex)=='\n') srcIndex++;
					case '\n': // ignore newlines immediately followed by backslash
						srcIndex++;
						continue;
					default: return '\\';
				}
				case '\r': if(source.codePointAt(srcIndex)=='\n') srcIndex++;
				case '\n': return '\n';
				case '/':
					if(ignoreComment) break;
					switch(source.codePointAt(srcIndex)){
						case '/':
							lineComment();
							continue;
						case '*':
							blockComment();
							continue;
						default: return '/';
					}
			}
			return c;
		}
	}

	private void lineComment(){
		while(true){
			switch(source.codePointAt(++srcIndex)){
				case '\r': case '\n': case EOF: return;
				case '\\': switch(source.codePointAt(++srcIndex)){
					case '\r': if(source.codePointAt(++srcIndex)!='\n') srcIndex--;
					case '\n': break;
				}
			}
		}
	}

	private void blockComment(){
		while(true){
			switch(source.codePointAt(++srcIndex)){
				case '*':
					if(source.codePointAt(srcIndex+1)=='/'){
						srcIndex += 2;
						return;
					}
				case EOF:
					parser.reportError("Unterminated block comment", srcIndex);
					return;
			}
		}
	}

	private int nextLiteralChar(){
		int c = nextCodePoint();
		return c=='\\' ? backslash() : c;
	}

	private int backslash(){
		int c2 = source.codePointAt(srcIndex++);
		switch(c2){
			case 'n': return '\n';
			case 't': return '\t';
			case 'r': return '\r';
			case 'b': return '\b';
			case 'f': return '\f';
			case 'u': return hex(4); // \ uxxxx // wait, why is this compilation error??????????
			case 'U': return hex(6); // \Uxxxxxx
			default: return c2; // just append trailing character
		}
	}

	private int hex(int digits){
		int start = srcIndex;
		int codePoint = 0;
		for(int i = 0; i<digits; i++){
			int n;
			int c = source.codePointAt(start+i);
			switch(c){
				case EOF:
					if(parser.engine().invalidUnicodeHandling!=ErrorHandling.IGNORE)
						parser.report(parser.engine().invalidUnicodeHandling==ErrorHandling.WARN ?
										ReportType.WARN : ReportType.ERROR,
								"Incomplete unicode escape", start+i);
					return source.codePointAt(start-1);
				case '0': n = 0; break;
				case '1': n = 1; break;
				case '2': n = 2; break;
				case '3': n = 3; break;
				case '4': n = 4; break;
				case '5': n = 5; break;
				case '6': n = 6; break;
				case '7': n = 7; break;
				case '8': n = 8; break;
				case '9': n = 9; break;
				case 'A': case 'a': n = 10; break;
				case 'B': case 'b': n = 11; break;
				case 'C': case 'c': n = 12; break;
				case 'D': case 'd': n = 13; break;
				case 'E': case 'e': n = 14; break;
				case 'F': case 'f': n = 15; break;
				default:
					if(parser.engine().invalidUnicodeHandling!=ErrorHandling.IGNORE){
						ReportType type = parser.engine().invalidUnicodeHandling==ErrorHandling.WARN ?
								ReportType.WARN : ReportType.ERROR;
						String message = new StringBuilder().append("Invalid character '")
								.appendCodePoint(c)
								.append("' for unicode escape").toString();
						parser.report(type, message, start+i);
					}
					return source.codePointAt(start-1);
			}
			codePoint |= n<<4*(digits-i-1);
		}
		if(codePoint>0x10FFFF||codePoint<0){
			if(parser.engine().invalidUnicodeHandling!=ErrorHandling.IGNORE)
				parser.report(parser.engine().invalidUnicodeHandling==ErrorHandling.WARN ?
								ReportType.WARN : ReportType.ERROR,
						"Provided value '"+Integer.toHexString(codePoint).toUpperCase(Locale.ROOT)+
								"' is outside the unicode range (0 ~ 10FFFF)",
						start);
			return source.codePointAt(start-1);
		}
		srcIndex += digits;
		return codePoint;
	}
}
