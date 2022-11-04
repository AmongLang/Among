package among.report;

import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link ReportHandler} that does nothing.
 */
public final class NoOpReportHandler implements ReportHandler{
	private static final NoOpReportHandler instance = new NoOpReportHandler();

	public static NoOpReportHandler instance(){
		return instance;
	}

	@Override public void report(ReportType type, String message, int srcIndex, @Nullable Throwable ex, String... hints){}

	@Override public ReportHandler reportAt(int sourcePosition){
		return this;
	}
}
