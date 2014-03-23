package miniJava.SyntacticAnalyzer;

import java.io.*;
import miniJava.Exceptions.*;

public class Scanner {

	private int col = 1;
	private int line = 1;
	private BufferedReader input;

	public Scanner(BufferedReader input) {
		this.input = input;
	}

	public Scanner(String input) {
		StringReader reader = new StringReader(input);
		this.input = new BufferedReader(reader);
	}

	/**
	 * Scans in input, returning next token.
	 * 
	 * @return
	 * @throws IOException
	 */
	public Token scan() throws ScanningException {

		String attr = "";
		Token token = null;

		while (token == null) {

			// Check for EOF
			int c = read();
			if (c == -1)
				return new Token("", Token.TYPE.EOT);

			// Setup
			attr += (char) c;

			switch (c) {

			// Operators
			case '*':
				token = new Token(attr, Token.TYPE.BINOP);
				break;

			case '+':
				if (peek('+'))
					throw new ScanningException(col, line);
				token = new Token(attr, Token.TYPE.BINOP);
				break;

			case '-':
				if (peek('-'))
					throw new ScanningException(col, line);
				token = new Token(attr, Token.TYPE.BINOP);
				break;

			// Check for comment
			case '/':
				if (peek('*')) {
					read();
					readComment();
					attr = "";
				} else if (peek('/')) {
					readLine();
					attr = "";
				} else
					token = new Token(attr, Token.TYPE.BINOP);
				break;

			// Check for c or c=
			case '>':
			case '<':
				if (peek('='))
					attr += (char) read();
				token = new Token(attr, Token.TYPE.BINOP);
				break;

			// Check for ! or !=
			case '!':
				if (!peek('='))
					token = new Token(attr, Token.TYPE.UNOP);
				else {
					attr += (char) read();
					token = new Token(attr, Token.TYPE.BINOP);
				}
				break;

			// Check for && or ||
			case '&':
			case '|':
				if (!peek((char) c))
					throw new ScanningException(col, line);
				else {
					attr += (char) read();
					token = new Token(attr, Token.TYPE.BINOP);
				}
				break;

			// Other Operators
			case '=':
				if (!peek('='))
					token = new Token(attr, Token.TYPE.EQUALS);
				else {
					attr += (char) read();
					token = new Token(attr, Token.TYPE.BINOP);
				}
				break;

			case '.':
				token = new Token(attr, Token.TYPE.PERIOD);
				break;

			case ',':
				token = new Token(attr, Token.TYPE.COMMA);
				break;

			case '[':
				token = new Token(attr, Token.TYPE.LSQUARE);
				break;

			case ']':
				token = new Token(attr, Token.TYPE.RSQUARE);
				break;

			case '{':
				token = new Token(attr, Token.TYPE.LBRACKET);
				break;

			case '}':
				token = new Token(attr, Token.TYPE.RBRACKET);
				break;

			case '(':
				token = new Token(attr, Token.TYPE.LPAREN);
				break;

			case ')':
				token = new Token(attr, Token.TYPE.RPAREN);
				break;

			case ';':
				token = new Token(attr, Token.TYPE.SEMICOLON);
				break;

			default:

				// Identifier or Keyword
				if (isAlpha((char) c)) {
					for (char n = peek(); isAlpha(n) || isDigit(n);) {
						attr += (char) read();
						n = peek();
					}

					if (Token.keywords.containsKey(attr)) {
						token = new Token(attr, Token.keywords.get(attr));
					} else {
						token = new Token(attr, Token.TYPE.ID);
					}
				}

				// Number
				else if (isDigit((char) c)) {
					for (char n = peek(); isDigit(n);) {
						attr += (char) read();
						n = peek();
					}

					token = new Token(attr, Token.TYPE.NUM);
				}

				// Whitespace
				else if (isWhitespace((char) c)) {
					attr = "";
				}

				// Unrecognized Character
				else
					throw new ScanningException(col, line);
				;

				break;
			}
		}

		token.posn = new SourcePosition(line, col - token.spelling.length());
		return token;
	}

	/**
	 * Looks at next character in stream without consuming.
	 * 
	 * @return
	 * @throws IOException
	 */
	private char peek() throws ScanningException {
		try {
			input.mark(1);
			int next = input.read();
			input.reset();

			return next == -1 ? '\0' : (char) next;
		} catch (IOException e) {
			throw new ScanningException(col, line);
		}
	}

	/**
	 * Returns whether passed character is next in stream.
	 * 
	 * @param c
	 * @return
	 * @throws IOException
	 */
	private boolean peek(char c) throws ScanningException {
		try {
			input.mark(1);
			int next = input.read();
			input.reset();

			return c == next;
		} catch (IOException e) {
			throw new ScanningException(col, line);
		}
	}

	/**
	 * Alternative reading that keeps track of position.
	 * 
	 * @return
	 * @throws IOException
	 */
	private int read() throws ScanningException {
		try {
			int next = input.read();
			if (next != '\n' && next != '\r')
				col += 1;
			else {
				col = 1;
				line += 1;
				if (peek('\r') || peek('\n'))
					next = input.read();
			}

			return next;
		} catch (IOException e) {
			throw new ScanningException(col, line);
		}
	}

	/**
	 * Consumes input until an end of comment has been reached.
	 * 
	 * @throws IOException
	 */
	private void readComment() throws ScanningException {
		char prev = '\0', current = '\0';
		while (prev != '*' || current != '/') {

			prev = current;

			int next = read();
			if (next == -1)
				throw new ScanningException(col, line);
			else
				current = (char) next;
		}
	}

	/**
	 * Consumes input until the end of line is reached
	 * 
	 * @throws IOException
	 */
	private void readLine() throws ScanningException {
		for (int n = 0; n != '\n' && n != '\r' && n != -1; n = read()) {
		}
	}

	/**
	 * Tells whether character is alphabetical.
	 * 
	 * @param c
	 * @return
	 */
	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
	}

	/**
	 * Tells whether character is numerical.
	 * 
	 * @param c
	 * @return
	 */
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * Tells wheter character is whitespace.
	 * 
	 * @param c
	 * @return
	 */
	private boolean isWhitespace(char c) {
		return c == ' ' || c == '\n' || c == '\r' || c == '\t';
	}

}
