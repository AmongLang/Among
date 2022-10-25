package ttmp.among.format;

import java.util.regex.Pattern;

public final class AmongLiteralFormatting{
	private AmongLiteralFormatting(){}

	private static final Pattern NEWLINE = Pattern.compile("\r\n?|\n");
	private static final Pattern BACKSPACE = Pattern.compile("\b");
	private static final Pattern FORMAT = Pattern.compile("\f");

	private static final Pattern SIMPLE_WORD = Pattern.compile("[^\\s:,{}\\[\\]()]+");
	private static final Pattern SIMPLE_MACRO_NAME = Pattern.compile("^(?!\\s)[^:{}\r\n]+(?<!\\s)$");
	private static final Pattern SIMPLE_KEY = Pattern.compile("^(?!\\s)[^:{}\r\n]+(?<!\\s)$");
	private static final Pattern SIMPLE_VALUE = Pattern.compile("^(?!\\s)[^:{}\r\n]+(?<!\\s)$");

	private static final Pattern PRIMITIVE_SPECIALS = Pattern.compile("[\\\\\"]|/[*/]");
	private static final Pattern PRIMITIVE_SPECIALS_JSON = Pattern.compile("[\\\\\"]");

	private static final Pattern WORD_SPECIALS = Pattern.compile("[\\s\\\\{}\\[\\]()\"',:]|/[*/]");
	private static final Pattern KEY_SPECIALS = Pattern.compile("[\\\\{}\\[\\]()\"',]|/[*/]");
	private static final Pattern MACRO_NAME_SPECIALS = Pattern.compile("[\\\\{}\\[\\]()\"',:]|/[*/]");
	private static final Pattern PARAM_SPECIALS = Pattern.compile("[\\s\\\\{}\\[\\]()\"',:=]|/[*/]");
	private static final Pattern VALUE_SPECIALS = Pattern.compile("[\\\\{}\\[\\]()\"',]|/[*/]");

	public static boolean isSimpleWord(String name){
		return SIMPLE_WORD.matcher(name).matches();
	}
	public static boolean isSimpleMacroName(String name){
		return SIMPLE_MACRO_NAME.matcher(name).matches();
	}
	public static boolean isSimpleKey(String key){
		return SIMPLE_KEY.matcher(key).matches();
	}
	public static boolean isSimpleValue(String value){
		return SIMPLE_VALUE.matcher(value).matches();
	}

	public static void simpleWordToString(StringBuilder stb, String name){
		stb.append(standardReplace(WORD_SPECIALS, name, true));
	}
	public static void simpleKeyToString(StringBuilder stb, String key, boolean paramRef){
		stb.append(standardReplace(KEY_SPECIALS, key, true));
	}
	public static void simpleMacroNameToString(StringBuilder stb, String name){
		stb.append(standardReplace(MACRO_NAME_SPECIALS, name, true));
	}

	public static void paramToString(StringBuilder stb, String param){
		stb.append(standardReplace(PARAM_SPECIALS, param, true));
	}

	public static void simpleValueToString(StringBuilder stb, String primitive){
		stb.append(standardReplace(VALUE_SPECIALS, primitive, true));
	}
	public static void primitiveToString(StringBuilder stb, String primitive){
		stb.append('"').append(standardReplace(PRIMITIVE_SPECIALS, primitive, true)).append('"');
	}

	public static void primitiveToPrettyString(StringBuilder stb, String primitive, int indents, PrettifyOption option){
		stb.append('"');
		if(option.jsonCompatibility){
			stb.append(standardReplace(PRIMITIVE_SPECIALS_JSON, primitive, true));
		}else{
			stb.append(NEWLINE.matcher(standardReplace(PRIMITIVE_SPECIALS, primitive, false))
					.replaceAll(newlineAndIndent(indents+2, option)+'|'));
		}
		stb.append('"');
	}

	public static String newlineAndIndent(int indents, PrettifyOption option){
		StringBuilder stb = new StringBuilder();
		newlineAndIndent(stb, indents, option);
		return stb.toString();
	}

	public static void newlineAndIndent(StringBuilder stb, int indents, PrettifyOption option){
		stb.append('\n');
		for(int i = 0; i<indents; i++) stb.append(option.indent);
	}

	private static String standardReplace(Pattern specialPattern, String value, boolean newline){
		value = specialPattern.matcher(value).replaceAll("\\\\$0");
		if(newline) value = NEWLINE.matcher(value).replaceAll("\\\\n");
		value = BACKSPACE.matcher(value).replaceAll("\\\\b");
		return FORMAT.matcher(value).replaceAll("\\\\f");
	}
}
