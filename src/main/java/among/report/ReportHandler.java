package among.report;

import among.Source;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Handler for any error or other type of information occurring in parsing.<br>
 * A report consists of an enum value indicating the severity ({@link ReportType}), a message, an optional source
 * position the report originated from, an optional exception the report originated from, and optional list of hints
 * provided for user convenience.<br>
 * A basic implementation can be easily created with {@link ReportHandler#simple()} or {@link
 * ReportHandler#custom(BiConsumer)}.
 *
 * @see Report
 * @see ReportType
 */
@FunctionalInterface
public interface ReportHandler{
	/**
	 * Generates a new report with severity of {@link ReportType#INFO}.
	 *
	 * @param message Message of the report
	 * @param hints   Optional list of hints provided for user convenience
	 * @see ReportHandler#report(ReportType, String, int, Throwable, String...)
	 */
	default void reportInfo(String message, String... hints){
		report(ReportType.INFO, message, hints);
	}

	/**
	 * Generates a new report with severity of {@link ReportType#INFO}.
	 *
	 * @param message  Message of the report
	 * @param srcIndex Source position the report originated from; negative values indicate no source position being
	 *                 provided
	 * @param hints    Optional list of hints provided for user convenience
	 * @see ReportHandler#report(ReportType, String, int, Throwable, String...)
	 */
	default void reportInfo(String message, int srcIndex, String... hints){
		report(ReportType.INFO, message, srcIndex, hints);
	}
	/**
	 * Generates a new report with severity of {@link ReportType#WARN}.
	 *
	 * @param message Message of the report
	 * @param hints   Optional list of hints provided for user convenience
	 * @see ReportHandler#report(ReportType, String, int, Throwable, String...)
	 */
	default void reportWarning(String message, String... hints){
		report(ReportType.WARN, message, hints);
	}

	/**
	 * Generates a new report with severity of {@link ReportType#WARN}.
	 *
	 * @param message  Message of the report
	 * @param srcIndex Source position the report originated from; negative values indicate no source position being
	 *                 provided
	 * @param hints    Optional list of hints provided for user convenience
	 * @see ReportHandler#report(ReportType, String, int, Throwable, String...)
	 */
	default void reportWarning(String message, int srcIndex, String... hints){
		report(ReportType.WARN, message, srcIndex, hints);
	}

	/**
	 * Generates a new report with severity of {@link ReportType#ERROR}.
	 *
	 * @param message Message of the report
	 * @param hints   Optional list of hints provided for user convenience
	 * @see ReportHandler#report(ReportType, String, int, Throwable, String...)
	 */
	default void reportError(String message, String... hints){
		report(ReportType.ERROR, message, hints);
	}

	/**
	 * Generates a new report with severity of {@link ReportType#ERROR}.
	 *
	 * @param message  Message of the report
	 * @param srcIndex Source position the report originated from; negative values indicate no source position being
	 *                 provided
	 * @param hints    Optional list of hints provided for user convenience
	 * @see ReportHandler#report(ReportType, String, int, Throwable, String...)
	 */
	default void reportError(String message, int srcIndex, String... hints){
		report(ReportType.ERROR, message, srcIndex, hints);
	}

	/**
	 * Generates a new report.
	 *
	 * @param type    Severity of the report
	 * @param message Message of the report
	 * @param hints   Optional list of hints provided for user convenience
	 * @see ReportHandler#report(ReportType, String, int, Throwable, String...)
	 */
	default void report(ReportType type, String message, String... hints){
		report(type, message, null, hints);
	}

	/**
	 * Generates a new report.
	 *
	 * @param type     Severity of the report
	 * @param message  Message of the report
	 * @param srcIndex Source position the report originated from; negative values indicate no source position being
	 *                 provided
	 * @param hints    Optional list of hints provided for user convenience
	 * @see ReportHandler#report(ReportType, String, int, Throwable, String...)
	 */
	default void report(ReportType type, String message, int srcIndex, String... hints){
		report(type, message, srcIndex, null, hints);
	}

	/**
	 * Generates a new report.
	 *
	 * @param type    Severity of the report
	 * @param message Message of the report
	 * @param ex      Optional exception the report originated from
	 * @param hints   Optional list of hints provided for user convenience
	 * @see ReportHandler#report(ReportType, String, int, Throwable, String...)
	 */
	default void report(ReportType type, String message, @Nullable Throwable ex, String... hints){
		report(type, message, -1, ex, hints);
	}

	/**
	 * Generates a new report.
	 *
	 * @param type     Severity of the report
	 * @param message  Message of the report
	 * @param srcIndex Source position the report originated from; negative values indicate no source position being
	 *                 provided
	 * @param ex       Optional exception the report originated from
	 * @param hints    Optional list of hints provided for user convenience
	 */
	void report(ReportType type, String message, int srcIndex, @Nullable Throwable ex, String... hints);

	/**
	 * Create a new report handler with specified 'default source position'; report will use specified source position
	 * if source position is not specified.
	 *
	 * @param sourcePosition Default source position
	 * @return New report handler with specified default source position
	 */
	default ReportHandler reportAt(int sourcePosition){
		return (type, message, srcIndex, ex, hints) ->
				report(type, message, srcIndex<0 ? sourcePosition : srcIndex, ex, hints);
	}

	/**
	 * Create a simple report handler with no source attached.<br>
	 * The report handler will print reports to {@link System#out} if report is type of {@link ReportType#INFO} or
	 * {@link ReportType#WARN}. The report handler will print reports to {@link System#err} if report is type of {@link
	 * ReportType#ERROR}.
	 *
	 * @return Simple report handler with no source attached
	 * @see ReportHandler#simple(Source)
	 */
	static ReportHandler simple(){
		return simple(null);
	}

	/**
	 * Create a simple report handler with given source. If source is {@code null}, no source will be attached.<br>
	 * The report handler will print reports to {@link System#out} if report is type of {@link ReportType#INFO} or
	 * {@link ReportType#WARN}. The report handler will print reports to {@link System#err} if report is type of {@link
	 * ReportType#ERROR}.
	 *
	 * @param src Source to be attached to the report handler
	 * @return Simple report handler with no source attached
	 */
	static ReportHandler simple(@Nullable Source src){
		return new SimpleReportHandler(src, -1);
	}

	/**
	 * Create a custom report handler with no source attached.
	 *
	 * @param printer Printer for any messages in report
	 * @return Custom report handler with no source attached
	 * @see ReportHandler#custom(Source, BiConsumer)
	 */
	static ReportHandler custom(BiConsumer<ReportType, String> printer){
		return custom(null, printer);
	}

	/**
	 * Create a custom report handler with given source. If source is {@code null}, no source will be attached.
	 *
	 * @param src     Source to be attached to the report handler
	 * @param printer Printer for any messages in report
	 * @return Custom report handler with no source attached
	 */
	static ReportHandler custom(@Nullable Source src, BiConsumer<ReportType, String> printer){
		return new CustomReportHandler(src, printer, -1);
	}

	/**
	 * Return a report handler that ignores all reports.
	 *
	 * @return Report handler that ignores all reports
	 */
	static ReportHandler ignore(){
		return NoOpReportHandler.instance();
	}
}
