package miniJava.SyntacticAnalyzer;

public class SourcePosition {

	public final int col;
	public final int line;

	public SourcePosition(int line, int col) {
		this.col = col;
		this.line = line;
	}

	@Override
	public String toString() {
		return "(Line: " + line + ", Column: " + col + ")";
	}
}
