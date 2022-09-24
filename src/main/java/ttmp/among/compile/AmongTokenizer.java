package ttmp.among.compile;

import org.jetbrains.annotations.Nullable;
import ttmp.among.compile.AmongToken.TokenType;
import ttmp.among.obj.AmongRoot;
import ttmp.among.util.OperatorRegistry;
import ttmp.among.util.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static ttmp.among.util.Source.EOF;

/**
 * Object responsible for converting raw source strings into list of {@link AmongToken}s.<br>
 * The tokenization process is mode dependant - same input might produce different result depending on the mode.
 * To compensate for the possible ambiguity of the compilation, a rudimentary 'setback' functionality is included,
 * which act as a kind of lookahead. (ok i know these ramblings sounded absolutely fucking terrible but its my child ok
 * dont be a dick and move on)
 *
 * @see AmongToken
 * @see TokenizationMode
 */
public final class AmongTokenizer{
	private final Source source;
	private final AmongParser parser;
	private final AmongRoot root;

	private int srcIndex;

	private final List<AmongToken> tokens = new ArrayList<>();
	private int tokenIndex;
	private int lastSrcIndex;

	public AmongTokenizer(Source source, AmongParser parser, AmongRoot root){
		this.source = source;
		this.parser = parser;
		this.root = root;
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
	public AmongToken next(boolean skipLineBreak, TokenizationMode mode){
		return next(skipLineBreak, mode, false);
	}
	/**
	 * Advances to the next token. New token is read if necessary.
	 *
	 * @param skipLineBreak If {@code true}, line break tokens will be skipped.
	 * @param mode          Mode for interpreting literal expressions.
	 * @param macro         Whether it will try to read {@link TokenType#PARAM_REF} tokens.
	 * @return The next token
	 */
	public AmongToken next(boolean skipLineBreak, TokenizationMode mode, boolean macro){
		while(true){
			AmongToken token = advance(mode, macro);
			if(!skipLineBreak||token.type!=TokenType.BR) return token;
		}
	}

	/**
	 * @return Last token read and returned with {@link AmongTokenizer#next(boolean, TokenizationMode, boolean) next()},
	 * or {@code null} if there isn't one (because either {@link AmongTokenizer#reset(boolean) reset()} was called or
	 * nothing was read yet)
	 */
	@Nullable public AmongToken lastToken(){
		return lastToken;
	}

	@Nullable private AmongToken lastToken;

	/**
	 * Advances token index; Returns EOF if it's already at the end.
	 */
	private AmongToken advance(TokenizationMode mode, boolean macro){
		if(tokenIndex>=tokens.size()){
			read(mode, macro);
			if(tokenIndex>=tokens.size())
				return lastToken = new AmongToken(TokenType.EOF, srcIndex);
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
			tokens.clear();
			srcIndex = lastSrcIndex;
		}
		tokenIndex = 0;
		lastToken = null;
	}

	private void read(TokenizationMode mode, boolean macro){
		while(true){
			int idx = srcIndex;
			int c = nextCodePoint();
			switch(c){
				case EOF: return;
				case ' ': case '\t': continue;
				case '\n':
					tokens.add(new AmongToken(TokenType.BR, idx));
					return;
				case '(':
					if(mode==TokenizationMode.KEY) break;
					tokens.add(new AmongToken(TokenType.L_PAREN, idx));
					return;
				case ')':
					if(mode==TokenizationMode.KEY) break;
					tokens.add(new AmongToken(TokenType.R_PAREN, idx));
					return;
				case '{':
					tokens.add(new AmongToken(TokenType.L_BRACE, idx));
					return;
				case '}':
					tokens.add(new AmongToken(TokenType.R_BRACE, idx));
					return;
				case '[':
					if(mode==TokenizationMode.KEY) break;
					tokens.add(new AmongToken(TokenType.L_BRACKET, idx));
					return;
				case ']':
					if(mode==TokenizationMode.KEY) break;
					tokens.add(new AmongToken(TokenType.R_BRACKET, idx));
					return;
				case ':':
					if(mode==TokenizationMode.VALUE||mode==TokenizationMode.NAME) break;
					tokens.add(new AmongToken(TokenType.COLON, idx));
					return;
				case ',':
					tokens.add(new AmongToken(TokenType.COMMA, idx));
					return;
				case '\'':
					tokens.add(new AmongToken(TokenType.COMPLEX_PRIMITIVE, idx, primitive('\'')));
					return;
				case '"':
					tokens.add(new AmongToken(TokenType.COMPLEX_PRIMITIVE, idx, primitive('"')));
					return;
				case '=':
					if(mode==TokenizationMode.PARAM){
						tokens.add(new AmongToken(TokenType.EQ, idx));
						return;
					}
			}
			if(mode==TokenizationMode.UNEXPECTED){
				tokens.add(new AmongToken(TokenType.ERROR, idx));
				return;
			}
			srcIndex = idx;
			switch(mode){
				case WORD:
					tokens.add(new AmongToken(TokenType.WORD, idx, word(true, false)));
					return;
				case NAME:
					tokens.add(new AmongToken(macro&&c=='$' ? TokenType.PARAM_REF : TokenType.NAME, idx, word(false, false)));
					return;
				case KEY:
					tokens.add(new AmongToken(macro&&c=='$' ? TokenType.PARAM_REF : TokenType.KEY, idx, multipleWords(true)));
					return;
				case PARAM:
					tokens.add(new AmongToken(macro&&c=='$' ? TokenType.PARAM_REF : TokenType.PARAM_NAME, idx, word(false, true)));
					return;
				case VALUE:
					tokens.add(new AmongToken(macro&&c=='$' ? TokenType.PARAM_REF : TokenType.VALUE, idx, multipleWords(false)));
					return;
				case OPERATION:
					operation(macro);
					return;
			}
		}
	}

	private String primitive(int closure){
		StringBuilder stb = new StringBuilder();
		while(true){
			int c = nextCodePoint();
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

	private String word(boolean word, boolean param){
		StringBuilder stb = new StringBuilder();
		int prev;
		L:
		while(true){
			prev = srcIndex;
			int c = nextCodePoint();
			switch(c){
				case '\\':
					stb.appendCodePoint(backslash());
					continue;
				case '=':
					if(param) break L;
					else break;
				case ':':
					if(word) break L;
					else break;
				case EOF: case ' ': case '\t': case '\n': case ',':
				case '{': case '}': case '[': case ']': case '(': case ')':
					break L;
			}
			stb.appendCodePoint(c);
		}
		srcIndex = prev;
		return stb.toString();
	}

	private String multipleWords(boolean key){
		StringBuilder stb = new StringBuilder();
		int lastNonWhitespaceSeen = srcIndex;
		int prev;
		L:
		while(true){
			prev = srcIndex;
			switch(nextCodePoint()){
				case ' ': case '\t': continue;
				case ':':
					if(key) break L;
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
		return stb.toString();
	}

	private void operation(boolean macro){
		int start = srcIndex;
		OperatorRegistry.Operator keyword = match(true);
		OperatorRegistry.Operator operator = null;

		int nameStart = srcIndex;
		int operatorStart = srcIndex;

		StringBuilder stb = new StringBuilder();
		L:
		while(true){
			int prev = srcIndex;
			int c = nextCodePoint();
			switch(c){
				case EOF: case ' ': case '\t': case '\n': case ':': case ',':
				case '{': case '}': case '[': case ']': case '(': case ')':
					srcIndex = prev;
					break L;
			}
			srcIndex = prev;
			operator = match(false);
			if(operator!=null){
				operatorStart = prev;
				break;
			}
			if(keyword!=null){
				stb.append(keyword.name());
				keyword = null;
			}
			stb.appendCodePoint(nextLiteralChar());
		}
		if(keyword!=null) tokens.add(new AmongToken(TokenType.KEYWORD, start, keyword.name()));
		else if(stb.length()>0)
			tokens.add(new AmongToken(macro&&source.codePointAt(nameStart)=='$' ? TokenType.PARAM_REF : TokenType.NAME, nameStart, stb.toString()));
		if(operator!=null) tokens.add(new AmongToken(TokenType.OPERATOR, operatorStart, operator.name()));
	}

	@Nullable private OperatorRegistry.Operator match(boolean keyword){
		int prev = srcIndex;
		Set<OperatorRegistry.Operator> set = keyword ?
				root.operators().getKeywords(nextLiteralChar()) :
				root.operators().getOperators(nextLiteralChar());
		srcIndex = prev;
		if(!set.isEmpty())
			for(OperatorRegistry.Operator o : set)
				if(matches(o)) return o;
		return null;
	}

	private boolean matches(OperatorRegistry.Operator operator){
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
				case '/': switch(source.codePointAt(srcIndex)){
					case '/':
						lineComment();
						continue;
					case '*':
						blockComment();
						continue;
					default: return '/';
				}
				default: return c;
			}
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
			case 'u':{ // \ uxxxx // wait, why is this compilation error??????????
				int r = hex(srcIndex, 4);
				srcIndex += 4;
				return r;
			}
			case 'U':{ // \Uxxxxxxxx
				int r = hex(srcIndex, 8);
				srcIndex += 8;
				return r;
			}
			default: return c2; // just append trailing character
		}
	}

	private int hex(int start, int digits){
		StringBuilder stb = new StringBuilder();
		for(int i = 0; i<digits; i++)
			stb.appendCodePoint(source.codePointAt(start+i));
		return Integer.parseUnsignedInt(stb.toString(), 16);
	}
}
