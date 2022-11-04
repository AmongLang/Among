package among.report;

import among.LnCol;
import among.Source;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Object representation of report handled with {@link ReportHandler}. A report consists of an enum value indicating the
 * severity ({@link ReportType}), a message, an optional source position the report originated from, an optional
 * exception the report originated from, and optional list of hints provided for user convenience.
 *
 * @see ReportType
 * @see ReportHandler
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

	/**
	 * Returns line and column of this report's source position. If this report does not have source position attached,
	 * {@code null} will be returned instead.
	 *
	 * @param source Source
	 * @return Line and column of this report's source position
	 */
	@Nullable public LnCol getLineColumn(Source source){
		return hasSourcePosition() ? source.getLnCol(sourcePosition) : null;
	}

	/**
	 * Prints the content of this report to the {@code logger}. Each string emitted to {@code logger} is expected to be
	 * displayed as separate lines.
	 *
	 * @param source Optional source for additional information, such as line/column index and code snippets
	 * @param logger Consumer for each line of the message
	 */
	public void print(@Nullable Source source, Consumer<String> logger){
		LnCol lc = source!=null ? getLineColumn(source) : null;

		logger.accept((lc!=null ? "["+lc+"] " : "")+type.toString()+": "+message);
		if(exception!=null){
			StringWriter w = new StringWriter();
			exception.printStackTrace(new PrintWriter(w));
			logger.accept(w.toString());
		}
		if(lc!=null){
			logger.accept(" "+lc.line+" |"+getLineSnippet(sourcePosition, source));
		}
		for(String hint : this.hints) logger.accept("hint: "+hint);
	}

	@Override public String toString(){
		return "Report{"+
				"type="+type+
				", message='"+message+'\''+
				", sourcePosition="+sourcePosition+
				", exception="+exception+
				", hints="+hints+
				'}';
	}

	/**
	 * Return part of a line around {@code sourcePosition}, taken from {@code source}. The specific position will be
	 * highlighted with commented inserted between.
	 *
	 * @param sourcePosition Codepoint index
	 * @param source         Source
	 * @return Part of a line around {@code sourcePosition}
	 * @throws IndexOutOfBoundsException If {@code sourcePosition < 0}
	 */
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
