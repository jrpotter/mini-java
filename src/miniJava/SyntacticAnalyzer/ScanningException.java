package miniJava.SyntacticAnalyzer;

import java.io.IOException;

/**
 *
 */
public class ScanningException extends IOException {

    private static final long serialVersionUID = 1L;

    public ScanningException(SourcePosition posn) {
        super("Scanning error at " + posn);
    }

}
