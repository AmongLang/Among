package among;

public enum ReportType{
	/**
	 * Report type indicating a simple, general purpose information.
	 */
	INFO,
	/**
	 * Report type indicating a negligible, yet suspicious observation.
	 */
	WARN,
	/**
	 * Report type indicating a critical failure occurred during operation.
	 */
	ERROR;

	@Override public String toString(){
		switch(this){
			case INFO: return "Info";
			case WARN: return "Warn";
			case ERROR: return "Error";
			default: throw new IllegalStateException("Unreachable");
		}
	}
}
