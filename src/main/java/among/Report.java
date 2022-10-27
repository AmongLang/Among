package among;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Message with type(info, warn or error) and position in source (optional), and exception (optional).
 */
public final class Report{
	private final ReportType type;
	private final String message;
	private final int sourcePosition;
	@Nullable private final Throwable exception;
	private final List<String> hints;

	public Report(ReportType type, String message, int sourcePosition, @Nullable Throwable exception, String... hints){
		this.type = type;
		this.message = message;
		this.sourcePosition = sourcePosition;
		this.exception = exception;
		this.hints = new ArrayList<>();
		Collections.addAll(this.hints, hints);
	}

	public ReportType type(){
		return type;
	}
	public String message(){
		return message;
	}
	public int sourcePosition(){
		return sourcePosition;
	}
	public boolean hasSourcePosition(){
		return sourcePosition>=0;
	}
	@Nullable public Throwable exception(){
		return exception;
	}
	public List<String> hints(){
		return Collections.unmodifiableList(hints);
	}

	@Nullable public LnCol getLineColumn(Source source){
		return hasSourcePosition() ? source.getLnCol(sourcePosition) : null;
	}

	public void print(Source source, Consumer<String> logger){
		LnCol lc = getLineColumn(source);

		logger.accept((lc!=null ? "["+lc+"] " : "")+type.toString()+": "+message);
		if(exception!=null){
			logger.accept(exception.toString());
			exception.printStackTrace();
		}
		if(lc!=null){
			logger.accept(" "+lc.line+" |"+getLineSnippet(sourcePosition, source));
		}
		for(String hint : this.hints) logger.accept("hint: "+hint);
	}

	public static String getLineSnippet(int sourcePosition, Source source){
		int line = source.lineAt(sourcePosition);
		int lineStart = source.lineStart(line);
		int lineSize = source.lineSize(line);

		if(lineSize>50){
			lineStart = Math.max(lineStart, sourcePosition-30);
			lineSize = 50;
		}

		StringBuilder stb = new StringBuilder();
		int i = 0;
		for(; i<lineSize; i++){
			int c = source.codePointAt(lineStart+i);
			if(c==Source.EOF) break;
			if(lineStart+i==sourcePosition) stb.append("/* HERE >>> */");
			if(c!='\r'&&c!='\n') stb.appendCodePoint(c);
		}
		if(lineStart+i<=sourcePosition) stb.append("  // <<< HERE");
		return stb.toString();
	}
}
