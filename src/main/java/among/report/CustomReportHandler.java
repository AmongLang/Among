package among.report;

import among.Source;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public class CustomReportHandler implements ReportHandler{
	@Nullable private final Source source;
	private final BiConsumer<ReportType, String> printer;
	private final int sourcePosition;

	public CustomReportHandler(@Nullable Source source, BiConsumer<ReportType, String> printer, int sourcePosition){
		this.source = source;
		this.printer = printer;
		this.sourcePosition = sourcePosition;
	}

	@Override public void report(ReportType type, String message, int srcIndex, @Nullable Throwable ex, String... hints){
		Report r = new Report(type, message, srcIndex, ex, hints);
		r.print(source, s -> printer.accept(type, s));
	}

	@Override public ReportHandler reportAt(int sourcePosition){
		return this.sourcePosition<0&&sourcePosition<0||this.sourcePosition==sourcePosition ?
				this : new CustomReportHandler(source, printer, sourcePosition);
	}
}
