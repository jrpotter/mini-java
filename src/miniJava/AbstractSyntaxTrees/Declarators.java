package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

/**
 * The following is a custom class, included for use with method and field
 * declarations.
 */
public class Declarators {

    public Declarators(boolean isPrivate, boolean isStatic, Type mt, SourcePosition posn) {
        this.isPrivate = isPrivate;
        this.isStatic = isStatic;
        this.mt = mt;
        this.posn = posn;
    }

    public boolean isPrivate;
    public boolean isStatic;
    public Type mt;
    public SourcePosition posn;
}