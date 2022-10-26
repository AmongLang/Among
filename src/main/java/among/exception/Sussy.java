package among.exception;

// feels like i'm having illegal amount of fun out of this stupid project name

/**
 * Base exception class for all Among language related exceptions.
 */
public class Sussy extends RuntimeException{
	public Sussy(){}
	public Sussy(String message){
		super(message);
	}
	public Sussy(String message, Throwable cause){
		super(message, cause);
	}
	public Sussy(Throwable cause){
		super(cause);
	}
	public Sussy(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace){
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
