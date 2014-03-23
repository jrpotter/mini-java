package miniJava.Exceptions;

public class ScanningException extends Exception {

	private static final long serialVersionUID = 1L;

	public ScanningException(int col, int line) {
		super("Scanning error at Column: " + col + ", Line: " + line);
	}

}
