package among.report;

import among.Source;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link ReportHandler} that prints report to {@link System#out} (on {@link ReportType#INFO INFO} or
 * {@link ReportType#WARN WARN}) or {@link System#err} (on {@link ReportType#ERROR ERROR}).
 *
 * @see ReportHandler#simple()
 * @see ReportHandler#simple(Source)
 */
public class SimpleReportHandler implements ReportHandler{
	@Nullable private final Source source;
	private final int sourcePosition;

	public SimpleReportHandler(@Nullable Source source, int sourcePosition){
		this.source = source;
		this.sourcePosition = sourcePosition;
	}

	@Override public void report(ReportType type, String message, int srcIndex, @Nullable Throwable ex, String... hints){
		Report r = new Report(type, message, srcIndex, ex, hints);
		r.print(source, type==ReportType.ERROR ? System.err::println : System.out::println);
	}

	@Override public ReportHandler reportAt(int sourcePosition){
		return this.sourcePosition<0&&sourcePosition<0||this.sourcePosition==sourcePosition ?
				this : new SimpleReportHandler(source, sourcePosition);
	}
}
