package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;

enum ErrorType {
	THIS,
	LENGTH,
	VOID_TYPE,
	CLASS_IDENTIFER,
	VARDECL_USED,
	NONFUNCTION_CALL,
	FUNCTION_ASSIGNMENT,
	UNDEFINED,
	STATIC, 
	VISIBILITY, 
	NO_RETURN, 
	TYPE_MISMATCH, 
	REDEFINITION, 
	MAIN_UNDECLARED, 
	INVALID_PARAM_COUNT, 
	MULTIPLE_MAIN, 
	UNDECLARED_TYPE, 
	SINGLE_VARCOND, 
	INVALID_INDEX
}

public class Reporter {

	public static boolean error = false;

	/**
	 * Convenience function for getting type names.
	 * 
	 * @param t
	 * @return
	 */
	private static String getTypeName(Type t) {
		if (t instanceof ClassType) {
			ClassType ct = (ClassType) t;
			return ct.className.spelling;
		} else if (t instanceof ArrayType) {
			ArrayType at = (ArrayType) t;
			return getTypeName(at.eltType);
		}

		return t.typeKind.toString();
	}

	/**
	 * Convenience method for formatting error message.
	 * 
	 * @param message
	 */
	private static void emit(String message) {
		System.out.println("***" + message);
	}

	/**
	 * Convenience function for managing all error types.
	 * 
	 * @param type
	 * @param a1
	 * @param a2
	 */
	public static void report(ErrorType type, AST a1, AST a2) {

		switch (type) {
		
			// Cannot access 'this' in a static method
			case THIS: {
				MethodDecl md = (MethodDecl) a2;
				emit("Cannot reference 'this' " + a1.posn + " in static method '" + md.name + "' " + md.posn);
				break;
			}
			
			// Array types have the single field 'length'
			case LENGTH: {
				emit("Array types have only a single field 'length' (at " + a1.posn + ").");
				break;
			}
			
			// Can't use a class as an identifier solely
			case CLASS_IDENTIFER: {
				emit("Cannot use class identifier outside of a qualified reference at " + a1.posn);
				break;
			}
			
			// Cannot have a parameter of type void
			case VOID_TYPE: {
				emit("Cannot have a parameter of type void at " + a1.posn);
				break;
			}
		
			// Attempting to call a non function as a function
			case NONFUNCTION_CALL: {
				emit("Not a valid function call at " + a1.posn);
				break;
			}
			
			// Cannot assign a value to a function
			case FUNCTION_ASSIGNMENT: {
				emit("Cannot assign a value to a function at " + a1.posn);
				break;
			}
	
			// Tried accessing a non-static member from a static method
			case STATIC: {
				MemberDecl md = (MemberDecl) a1;
				Identifier ident = (Identifier) a2;
				emit("'" + md.name + "' " + md.posn + " is an instance member and cannot be accessed at " + ident.posn);
				break;
			}
	
			// Tried accessing a private member of a different class
			case VISIBILITY: {
				MemberDecl md = (MemberDecl) a1;
				Identifier ident = (Identifier) a2;
				emit("'" + md.name + "' " + md.posn + " is a private member and cannot be accessed at " + ident.posn);
				break;
			}
	
			// Non-void function does not have a return statement
			case NO_RETURN: {
				MethodDecl md = (MethodDecl) a1;
				emit("'" + md.name + "' " + md.posn + " must have a return statement");
				break;
			}
	
			// The passed types are not the same
			case TYPE_MISMATCH: {
				String name1 = getTypeName((Type) a1);
				String name2 = getTypeName((Type) a2);
				if(a1 instanceof ArrayType) name1 += " Array";
				if(a2 instanceof ArrayType) name2 += " Array";
				emit("Expected type '" + name1 + "' but got '" + name2 + "' " + a2.posn);
				break;
			}
	
			// Attempting to redeclare a variable
			case REDEFINITION: {
				emit("Variable at " + a1.posn + " already declared earlier at " + a2.posn);
				break;
			}
			
			// Identifier could not be found
			case UNDEFINED: {
				Identifier ident = (Identifier) a1;
				emit("Identifier '" + ident.spelling + "' " + ident.posn + " is undeclared.");
				break;
			}
	
			// A public static void main(String[] args) method was not declared
			case MAIN_UNDECLARED: {
				emit("A main function was not declared");
				break;
			}
	
			// Parameter counts of an expression/statement do not match declaration
			case INVALID_PARAM_COUNT: {
				MethodDecl md = (MethodDecl) a2;
				emit("Call to '" + md.name + "' " + a2.posn + " has an invalid parameter count at " + a1.posn);
				break;
			}
	
			// A public static void main(String[] args) was declared more than once
			case MULTIPLE_MAIN: {
				emit("Main function at " + a1.posn + " already declared previously at " + a2.posn);
				break;
			}
	
			// A reference has been made to a non-existant type
			case UNDECLARED_TYPE: {
				if(a1 instanceof Type) {
					String typeName = getTypeName((Type) a1);
					emit("'" + typeName + "' " + a1.posn + " has not been declared previously");
				} else {
					emit("Identifier at " + a1.posn + " could not be identified");
				}
				
				break;
			}
	
			// A Variable Declaration Statement was made as the only statement of a condition
			case SINGLE_VARCOND: {
				emit("Conditional statment cannot be followed by a variable declaration statement exclusively " + a1.posn);
				break;
			}
	
			// An indexed expression must be of an int type
			case INVALID_INDEX: {
				emit("Index expression is not of type int " + a1.posn);
				break;
			}
			
			// A variable declaration identifier was used in a var decl statement
			case VARDECL_USED: {
				emit("Identifier at " + a1.posn + " cannot refer to the variable declaration at " + a2.posn);
				break;
			}
		}

		error = true;
	}

}
