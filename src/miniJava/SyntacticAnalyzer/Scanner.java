package miniJava.SyntacticAnalyzer;

import java.io.*;

public class Scanner {

    private int col = 1;
    private int line = 1;
    private boolean predefined;
    private BufferedReader input;

    /**
     * 
     * @param input
     */
    public Scanner(BufferedReader input) {
        this(input, false);
    }

    /**
     * 
     * @param input
     * @param predefined
     */
    public Scanner(String input, boolean predefined) {
        this(new BufferedReader(new StringReader(input)), predefined);
    }

    /**
     * 
     * @param input
     * @param predefined
     */
    public Scanner(BufferedReader input, boolean predefined) {
        this.input = input;
        this.predefined = predefined;
    }

    // /////////////////////////////////////////////////////////////////////////////
    //
    // Scanning
    //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @return
     * @throws IOException
     */
    public Token scan() throws IOException {
        Token token = null;
        String spelling = "";

        while (token == null) {

            int c = read();
            SourcePosition posn = new SourcePosition(col, line);

            if (c == -1) {
                token = new Token("", Token.TYPE.EOT, posn);
            } else {
                spelling += (char) c;

                switch (c) {

                // Operators
                case '*':
                case '+':
                case '-': {
                    if (peek(c))
                        throw new ScanningException(posn);
                    token = new Token(spelling, Token.TYPE.BINOP, posn);
                    break;
                }

                    // Comment
                case '/': {
                    if (peek('*')) {
                        read();
                        readMultiLineComment();
                        spelling = "";
                    } else if (peek('/')) {
                        readSingleLineComment();
                        spelling = "";
                    } else {
                        token = new Token(spelling, Token.TYPE.BINOP, posn);
                    }

                    break;
                }

                    // Relational
                case '>':
                case '<': {
                    if (peek('='))
                        spelling += (char) read();
                    token = new Token(spelling, Token.TYPE.BINOP, posn);
                    break;
                }

                    // Negation
                case '!': {
                    if (peek('=')) {
                        spelling += (char) read();
                        token = new Token(spelling, Token.TYPE.BINOP, posn);
                    } else {
                        token = new Token(spelling, Token.TYPE.UNOP, posn);
                    }

                    break;
                }

                    // Logical
                case '&':
                case '|': {
                    if (!peek(c)) {
                        throw new ScanningException(posn);
                    } else {
                        spelling += (char) read();
                        token = new Token(spelling, Token.TYPE.BINOP, posn);
                    }

                    break;
                }

                    // Other Operators
                case '=': {
                    if (peek('=')) {
                        spelling += (char) read();
                        token = new Token(spelling, Token.TYPE.BINOP, posn);
                    } else {
                        token = new Token(spelling, Token.TYPE.EQUALS, posn);
                    }

                    break;
                }

                    // Miscellaneous
                case '.':
                case ',':
                case '[':
                case ']':
                case '{':
                case '}':
                case '(':
                case ')':
                case ';': {
                    token = new Token(spelling, Token.symbols.get(c), posn);
                    break;
                }

                default: {

                    // Identifier or keyword
                    if (isAlpha(c)) {
                        int next = peek();
                        while (isAlpha(next) || isDigit(next) || next == '_') {
                            spelling += (char) read();
                            next = peek();
                        }

                        if (Token.keywords.containsKey(spelling)) {
                            token = new Token(spelling, Token.keywords.get(spelling), posn);
                        } else {
                            token = new Token(spelling, Token.TYPE.ID, posn);
                        }
                    }

                    // Number
                    else if (isDigit(c)) {
                        int next = peek();
                        while (isDigit(next)) {
                            spelling += (char) read();
                            next = peek();
                        }

                        token = new Token(spelling, Token.TYPE.NUM, posn);
                    }

                    // Whitespace
                    else if (isWhitespace(c)) {
                        spelling = "";
                    }

                    // Unrecognized Character
                    else {
                        throw new ScanningException(posn);
                    }
                }
                }
            }
        }

        return token;
    }

    // /////////////////////////////////////////////////////////////////////////////
    //
    // Convenience Methods
    //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @param c
     * @return
     */
    private boolean isAlpha(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (predefined && c == '_');
    }

    /**
     * 
     * @param c
     * @return
     */
    private boolean isDigit(int c) {
        return c >= '0' && c <= '9';
    }

    /**
     * 
     * @param c
     * @return
     */
    private boolean isWhitespace(int c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private int peek() throws IOException {
        input.mark(1);
        int next = input.read();
        input.reset();

        return next;
    }

    /**
     * 
     * @param c
     * @return
     * @throws IOException
     */
    private boolean peek(int c) throws IOException {
        input.mark(1);
        int next = input.read();
        input.reset();

        return c == next;
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private int read() throws IOException {
        int next = input.read();
        if (next == '\n' || next == '\r') {
            col = 1;
            line += 1;
        } else {
            col += 1;
        }

        return next;
    }

    /**
     * 
     * @throws IOException
     */
    private void readSingleLineComment() throws IOException {
        col = 1;
        line += 1;
        input.readLine();
    }

    /**
     * 
     * @throws IOException
     */
    private void readMultiLineComment() throws IOException {
        int prev = '\0';
        int current = '\0';

        while (prev != '*' || current != '/') {
            prev = current;
            current = read();

            // Unterminated
            if (current == -1) {
                SourcePosition posn = new SourcePosition(line, col);
                throw new ScanningException(posn);
            }
        }
    }
}
