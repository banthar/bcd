package bdc;

public class ClassFormatException extends Exception {
    public ClassFormatException(final String message) {
	super(message);
    }

    public ClassFormatException(final String message, final ClassFormatException cause) {
	super(message, cause);
    }
}
