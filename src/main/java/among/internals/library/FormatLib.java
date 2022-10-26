package among.internals.library;

import among.obj.AmongPrimitive;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FormatLib{
	private FormatLib(){}

	private static final Pattern FORMAT_REGEX = Pattern.compile("\\{\\{}}|\\{((?:\\\\.|[^}])*)}");
	public static String format(String fmt, Object... args){
		StringBuilder stb = null;
		int last = 0, autoIndex = 0;
		for(Matcher m = FORMAT_REGEX.matcher(fmt); m.find(); last = m.end()){
			if(stb==null) stb = new StringBuilder();
			stb.append(fmt, last, m.start());

			String group = index(m, fmt);
			if(group==null) stb.append("{}");
			else if(group.isEmpty()){
				if(autoIndex<args.length) append(stb, args[autoIndex++]);
				else stb.append(fmt, m.start(), m.end());
			}else try{
				int i = Integer.parseInt(group);
				if(i>=0&&i<args.length) append(stb, args[i]);
				else stb.append(fmt, m.start(), m.end());
			}catch(NumberFormatException ex){
				stb.append(fmt, m.start(), m.end());
			}
		}
		if(stb==null) return fmt;
		stb.append(fmt, last, fmt.length());
		return stb.toString();
	}
	public static String format(String fmt, Map<String, ?> args){
		StringBuilder stb = null;
		int last = 0;
		for(Matcher m = FORMAT_REGEX.matcher(fmt); m.find(); last = m.end()){
			if(stb==null) stb = new StringBuilder();
			stb.append(fmt, last, m.start());

			String group = index(m, fmt);
			if(group==null) stb.append("{}");
			else if(args.containsKey(group)) append(stb, args.get(group));
			else stb.append(fmt, m.start(), m.end());
		}
		if(stb==null) return fmt;
		stb.append(fmt, last, fmt.length());
		return stb.toString();
	}

	@Nullable private static String index(Matcher matcher, String fmt){
		int start = matcher.start(1);
		if(start<0) return null;
		StringBuilder stb = new StringBuilder();
		for(int end = matcher.end(1); start<end; start++){
			if(fmt.charAt(start)=='\\') start++;
			stb.append(fmt.charAt(start));
		}
		return stb.toString();
	}

	private static void append(StringBuilder stb, Object o){
		stb.append(o instanceof AmongPrimitive ? ((AmongPrimitive)o).getValue() : o);
	}
}
