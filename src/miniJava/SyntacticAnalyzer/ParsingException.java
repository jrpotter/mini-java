package miniJava.SyntacticAnalyzer;

import java.io.IOException;

/**
 *
 */
public class ParsingException extends IOException {

    private static final long serialVersionUID = 1L;

    public ParsingException(SourcePosition posn) {
        super("Parsing error at " + posn);
    }

}