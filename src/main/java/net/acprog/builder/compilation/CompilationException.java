package net.acprog.builder.compilation;

/**
 * Exception indicating a compilation problem.
 */
@SuppressWarnings("serial")
public class CompilationException extends RuntimeException {

    public CompilationException() {
    }

    public CompilationException(String message) {
	super(message);
    }

    public CompilationException(Throwable cause) {
	super(cause);
    }

    public CompilationException(String message, Throwable cause) {
	super(message, cause);
    }

    public CompilationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
	super(message, cause, enableSuppression, writableStackTrace);
    }

}
