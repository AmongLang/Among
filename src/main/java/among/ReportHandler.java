package among;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ReportHandler{
	default void reportInfo(String message, String... hints){
		report(ReportType.INFO, message, hints);
	}
	default void reportInfo(String message, int srcIndex, String... hints){
		report(ReportType.INFO, message, srcIndex, hints);
	}
	default void reportWarning(String message, String... hints){
		report(ReportType.WARN, message, hints);
	}
	default void reportWarning(String message, int srcIndex, String... hints){
		report(ReportType.WARN, message, srcIndex, hints);
	}
	default void reportError(String message, String... hints){
		report(ReportType.ERROR, message, hints);
	}
	default void reportError(String message, int srcIndex, String... hints){
		report(ReportType.ERROR, message, srcIndex, hints);
	}

	default void report(ReportType type, String message, String... hints){
		report(type, message, null, hints);
	}
	default void report(ReportType type, String message, int srcIndex, String... hints){
		report(type, message, srcIndex, null, hints);
	}

	default void report(ReportType type, String message, @Nullable Throwable ex, String... hints){
		report(type, message, -1, ex, hints);
	}
	void report(ReportType type, String message, int srcIndex, @Nullable Throwable ex, String... hints);

	default ReportHandler reportAt(int sourcePosition){
		return (type, message, srcIndex, ex, hints) ->
				report(type, message, srcIndex<0 ? sourcePosition : srcIndex, ex, hints);
	}
}
