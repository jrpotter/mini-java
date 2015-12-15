package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.Declaration;

public class Reporter {

    public static boolean error = false;

    /**
     * 
     * @param message
     */
    public static void emit(String message) {
        error = true;
        System.out.println("***" + message);
    }

    /**
     * Redefinitions
     * 
     * @param d1
     * @param d2
     */
    public static void report(Declaration d1, Declaration d2, String prefix) {
        emit(prefix + " at " + d1.posn + " previously defined at " + d2.posn);
    }

}
