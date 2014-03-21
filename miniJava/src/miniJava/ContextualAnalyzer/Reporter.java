package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;

enum ErrorType {
	VAR_COND_ONLY, 		
	MISSING_DECL,
	UNIDENTIFIED,
	REDEFINITION,
	INVALID_PARAM_COUNT,
	INVALID_INDEX,
	TYPE_MISMATCH,
	UNIDENTIFIED_TYPE,
	TYPE_CLASS_MISMATCH,
	TYPE_ARRAY_MISMATCH,
	NO_RETURN_EXPRESSION;
}

public class Reporter {
	
	public static boolean error = false;
	
	/**
	 * Prints out to console correct error message.
	 * @param type
	 * @param ast
	 */
	public static void report(ErrorType type, AST ast) {
		error = true;
		switch(type) {
		
			// VarDeclStmt is only statement in conditional branch
			case VAR_COND_ONLY: {
				System.out.println("***Conditional statment cannot be followed by a variable declaration statement " + ast.posn);
				break;
			}
			
			// Declaration does not exist for reference
			case MISSING_DECL: {
				System.out.println("***Reference to a non-existant type " + ast.posn);
				break;
			}
			
			// Reports when a reference could not be found
			case UNIDENTIFIED: {
				System.out.println("***Reference refers to a declaration that does not exist " + ast.posn);
				break;
			}
			
			// Attempting to redeclare a variable
			case REDEFINITION: {
				System.out.println("***Variable has already been defined earlier " + ast.posn);
				break;
			}
			
			// A non void function does not have a return statement
			case NO_RETURN_EXPRESSION: {
				System.out.println("***Non-void method does not have a return statement " + ast.posn);
				break;
			}
			
			// The number of parameters passed is either too few or too great
			case INVALID_PARAM_COUNT: {
				System.out.println("***The number of passed parameters does not equal expected count " + ast.posn);
			}
			
			// The expected expression MUST return an int (such as the index of an array)
			case INVALID_INDEX: {
				System.out.println("***Expected an integer value as the index of an array " + ast.posn);
				break;
			}
			
			// Hmmm.....
			case UNIDENTIFIED_TYPE: {
				System.out.println("***Unexpected type " + ast.posn);
				break;
			}
			
			// Clear Warning
			default:
				break;
		}
	}
	
	/**
	 * Type specific error reporting.
	 * @param type
	 * @param t1
	 * @param t2
	 */
	public static void report(ErrorType type, Type t1, Type t2) {
		error = true;
		switch(type) {
		
			// Non class/array types don't match
			case TYPE_MISMATCH: {
				System.out.println("***Expected type " + t1.typeKind + " but got " + t2.typeKind + t2.posn);
				break;
			}
			
			// Two classes don't match
			case TYPE_CLASS_MISMATCH: {
				ClassType c1 = (ClassType) t1;
				ClassType c2 = (ClassType) t2;
				System.out.println("***Expected type " + c1.className.spelling + " but got " + c2.className.spelling + c2.posn);
				break;
			}
			
			// Two arrays don't match
			case TYPE_ARRAY_MISMATCH: {
				ArrayType a1 = (ArrayType) t1;
				ArrayType a2 = (ArrayType) t2;
				System.out.println("***Expected array type " + a1.eltType.typeKind + " but got " + a2.eltType.typeKind + t2.posn);
				break;
			}
			
			// Clear Warning
			default:
				break;
		}
	}
}
