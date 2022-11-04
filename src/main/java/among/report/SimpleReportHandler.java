package among.report;

import among.Source;
import org.jetbrains.annotations.Nullable;

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
