package miniJava.ContextualAnalyzer;

import java.util.*;

import miniJava.Compiler;
import miniJava.Exceptions.*;
import miniJava.SyntacticAnalyzer.*;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

public class Analyzer implements Visitor<IdentificationTable, Type> {

	private MethodDecl mainMethod = null;
	private ClassDecl currentClassDecl = null;
	private MethodDecl currentMethodDecl = null;
	private IdentificationTable table = new IdentificationTable();

	// Keep track of all predefined names to handle
	private static ArrayList<String> predefined;
	static 
	{
		predefined = new ArrayList<String>();
		predefined.add("class _PrintStream { public void println(int n){} }");
		predefined.add("class System {  public static _PrintStream out; }");
		predefined.add("class String { }");
	}

	/**
	 * Adds predefined names to topmost table. These names should not be allowed
	 * redefinition and must be ordered such that no class refers to one
	 * undeclared yet.
	 */
	public Analyzer() throws ParsingException, ScanningException {
		for (String s : predefined) {
			Scanner scanner = new Scanner(s);
			Parser parser = new Parser(scanner);
			parser.parse().visit(this, table);
		}
	}

	/**
	 * Checks that contextual analysis was successful or not, returning the
	 * proper error code in either case.
	 * 
	 * @return
	 */
	public int validate() {
		// Exactly one public static void main(String[] args) function must be declared
		if (mainMethod == null)
			Reporter.report(ErrorType.MAIN_UNDECLARED, null, null);

		return (Reporter.error) ? Compiler.rc : 0;
	}

	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitPackage(Package prog, IdentificationTable arg) {

		/*
		 * Since classes and static methods/fields can be referenced before the
		 * classes themselves are declared, we preprocess all classes and
		 * methods first.
		 */
		for (ClassDecl cd : prog.classDeclList) {

			IdentificationTable cdTable = table.openScope(cd);

			if (cdTable != null) {

				// Simply add fields to current scope
				for (FieldDecl fd : cd.fieldDeclList) {
					cdTable.setDeclarationAtScope(fd);
				}

				// Just Add Declaration (also creates another identification table)
				for (MethodDecl md : cd.methodDeclList) {
					cdTable.openScope(md);
				}
			}
		}

		// Begin Traversal
		for (ClassDecl cd : prog.classDeclList) {
			currentClassDecl = cd;
			cd.visit(this, table);
		}

		return null;
	}

	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitClassDecl(ClassDecl cd, IdentificationTable arg) {

		IdentificationTable cdTable = arg.getScope(cd);

		if (cdTable != null) {

			for (FieldDecl fd : cd.fieldDeclList) {
				fd.visit(this, cdTable);
			}

			for (MethodDecl md : cd.methodDeclList) {
				currentMethodDecl = md;
				md.visit(this, cdTable);
			}
		}

		return cd.type;
	}

	public Type visitFieldDecl(FieldDecl fd, IdentificationTable arg) {

		// Must check that the type of the field can be identified
		if (!table.classExists(fd.type)) {
			Reporter.report(ErrorType.UNDECLARED_TYPE, fd.type, null);
		}

		return fd.type;
	}

	public Type visitMethodDecl(MethodDecl md, IdentificationTable arg) {

		// Check if a valid entry point to program
		if (IdentificationTable.isMainMethod(md)) {
			if (mainMethod != null)
				Reporter.report(ErrorType.MULTIPLE_MAIN, md, mainMethod);
			else
				mainMethod = md;
		}

		// Must check that the type of the method can be identified
		if (!table.classExists(md.type)) {
			Reporter.report(ErrorType.UNDECLARED_TYPE, md.type, null);
		}

		// Continue Traversal
		IdentificationTable mdTable = arg.getScope(md);
		if (mdTable != null) {
			for (ParameterDecl pd : md.parameterDeclList)
				pd.visit(this, mdTable);
			for (Statement s : md.statementList)
				s.visit(this, mdTable);

			// Check that return type matches expected type
			if (md.returnExp == null && md.type.typeKind != TypeKind.VOID) {
				Reporter.report(ErrorType.NO_RETURN, md, null);
			} else if (md.returnExp != null) {
				Type returnType = md.returnExp.visit(this, mdTable);
				IdentificationTable.match(md.type, returnType, true);
			}
		}

		return md.type;
	}

	public Type visitParameterDecl(ParameterDecl pd, IdentificationTable arg) {
		arg.setDeclarationAtScope(pd);
		if (!table.classExists(pd.type)) {
			Reporter.report(ErrorType.UNDECLARED_TYPE, pd.type, null);
		}

		return pd.type;
	}

	public Type visitVarDecl(VarDecl decl, IdentificationTable arg) {
		arg.setDeclarationAtScope(decl);
		if (!table.classExists(decl.type)) {
			Reporter.report(ErrorType.UNDECLARED_TYPE, decl.type, null);
		}

		return decl.type;
	}
	

	// /////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitBaseType(BaseType type, IdentificationTable arg) {

		return type;
	}

	public Type visitClassType(ClassType type, IdentificationTable arg) {
		type.className.visit(this, arg);
		return type;
	}

	public Type visitArrayType(ArrayType type, IdentificationTable arg) {
		type.eltType.visit(this, arg);
		return type;
	}
	

	// /////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitBlockStmt(BlockStmt stmt, IdentificationTable arg) {
		arg.pushLevel();
		for (Statement s : stmt.sl)
			s.visit(this, arg);
		arg.popLevel();

		return null;
	}

	// The stmt of the vardecl may not refer to the variable itself so we check the stmt first
	public Type visitVardeclStmt(VarDeclStmt stmt, IdentificationTable arg) {
		Type initExpType = stmt.initExp.visit(this, arg);
		Type varDeclType = stmt.varDecl.visit(this, arg);
		IdentificationTable.match(varDeclType, initExpType, true);

		return varDeclType;
	}

	public Type visitAssignStmt(AssignStmt stmt, IdentificationTable arg) {
		Type valType = stmt.val.visit(this, arg);
		Type refType = stmt.ref.visit(this, arg);
		IdentificationTable.match(valType, refType, true);

		return refType;
	}

	public Type visitCallStmt(CallStmt stmt, IdentificationTable arg) {

		Type methodType = stmt.methodRef.visit(this, arg);

		// Check that parameter count is correct and each type is correct
		MethodDecl decl = (MethodDecl) stmt.methodRef.decl;
		if (decl.parameterDeclList.size() != stmt.argList.size()) {
			Reporter.report(ErrorType.INVALID_PARAM_COUNT, stmt, decl);
		} else {
			for (int i = 0; i < stmt.argList.size(); i++) {
				Type exprType = stmt.argList.get(i).visit(this, arg);
				Type pdType = decl.parameterDeclList.get(i).type;
				IdentificationTable.match(pdType, exprType, true);
			}
		}

		return methodType;
	}

	public Type visitIfStmt(IfStmt stmt, IdentificationTable arg) {

		// The conditional statment must be a boolean
		Type condType = stmt.cond.visit(this, arg);
		IdentificationTable.match(new BaseType(TypeKind.BOOLEAN, null), condType, true);

		// A single vardecl cannot exist after a conditional statement
		if (stmt.thenStmt instanceof VarDeclStmt) {
			Reporter.report(ErrorType.SINGLE_VARCOND, stmt.thenStmt, null);
		} else {
			stmt.thenStmt.visit(this, arg);
			if (stmt.elseStmt != null) {
				if (stmt.elseStmt instanceof VarDeclStmt) {
					Reporter.report(ErrorType.SINGLE_VARCOND, stmt.elseStmt,
							null);
				} else {
					stmt.elseStmt.visit(this, arg);
				}
			}
		}

		return null;
	}

	public Type visitWhileStmt(WhileStmt stmt, IdentificationTable arg) {

		// The conditional statment must be a boolean
		Type condType = stmt.cond.visit(this, arg);
		IdentificationTable.match(new BaseType(TypeKind.BOOLEAN, null), condType, true);

		// A single vardecl cannot exist after a conditional statement
		if (stmt.body instanceof VarDeclStmt) {
			Reporter.report(ErrorType.SINGLE_VARCOND, stmt.body, null);
		} else {
			stmt.body.visit(this, arg);
		}

		return null;
	}

	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitUnaryExpr(UnaryExpr expr, IdentificationTable arg) {
		Type opType = expr.operator.visit(this, arg);
		Type exprType = expr.expr.visit(this, arg);
		IdentificationTable.match(opType, exprType, true);

		return opType;
	}

	public Type visitBinaryExpr(BinaryExpr expr, IdentificationTable arg) {
		Type opType = expr.operator.visit(this, arg);
		Type leftType = expr.left.visit(this, arg);
		Type rightType = expr.right.visit(this, arg);
		
		// Both sides must be the same
		if(opType.typeKind == TypeKind.EQUALS) {
			IdentificationTable.match(leftType, rightType, true);
			return new BaseType(TypeKind.BOOLEAN, opType.posn);
		} 
		
		// Both sides must be integers
		else if(opType.typeKind == TypeKind.RELATIONAL) {
			BaseType bt = new BaseType(TypeKind.INT, opType.posn);
			IdentificationTable.match(bt, leftType, true);
			IdentificationTable.match(bt, rightType, true);
			return new BaseType(TypeKind.BOOLEAN, opType.posn);
		} 
		
		// Both sides must match operator type
		IdentificationTable.match(leftType, opType, true);
		IdentificationTable.match(rightType, opType, true);
		return opType;
	}

	public Type visitRefExpr(RefExpr expr, IdentificationTable arg) {
		Type exprType = expr.ref.visit(this, arg);
		return exprType;
	}

	public Type visitCallExpr(CallExpr expr, IdentificationTable arg) {
		Type functionRefType = expr.functionRef.visit(this, arg);
		
		if(expr.functionRef.decl instanceof MethodDecl) {
			
			MethodDecl decl = (MethodDecl) expr.functionRef.decl;
			
			// Check that parameter count is correct and each type is correct
			if (decl.parameterDeclList.size() != expr.argList.size()) {
				Reporter.report(ErrorType.INVALID_PARAM_COUNT, expr, decl);
			} else {
				for (int i = 0; i < expr.argList.size(); i++) {
					Type exprType = expr.argList.get(i).visit(this, arg);
					Type pdType = decl.parameterDeclList.get(i).type;
					IdentificationTable.match(pdType, exprType, true);
				}
			}
		} else {
			Reporter.report(ErrorType.NONFUNCTION_CALL, expr, null);
		}

		return functionRefType;
	}

	public Type visitLiteralExpr(LiteralExpr expr, IdentificationTable arg) {
		Type literalType = expr.literal.visit(this, arg);
		return literalType;
	}

	public Type visitNewObjectExpr(NewObjectExpr expr, IdentificationTable arg) {
		Type objectType = expr.classtype.visit(this, arg);
		return objectType;
	}

	public Type visitNewArrayExpr(NewArrayExpr expr, IdentificationTable arg) {
		Type sizeExprType = expr.sizeExpr.visit(this, arg);
		if (sizeExprType.typeKind != TypeKind.INT) {
			Reporter.report(ErrorType.INVALID_INDEX, expr.sizeExpr, null);
		}

		Type eltType = expr.eltType.visit(this, arg);
		return new ArrayType(eltType, expr.posn);
	}

	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitQualifiedRef(QualifiedRef ref, IdentificationTable arg) {
		
		Type refType = ref.ref.visit(this, arg);
		
		// Note qualified ref's only make sense in the context of classes
		if(refType.typeKind != TypeKind.CLASS) {
			if(refType.typeKind != TypeKind.ERROR) // Don't need to report multiple times
				Reporter.report(ErrorType.TYPE_MISMATCH, new BaseType(TypeKind.CLASS, null), refType);
			
			return refType;
		} 
		
		// Try to find qualified member
		Declaration qualified = ref.ref.decl;
		ClassType qualType = (ClassType) qualified.type;
		Declaration qualClassDecl = table.getDeclaration(qualType.className.spelling);
		if(qualClassDecl == null) {
			Reporter.report(ErrorType.UNDECLARED_TYPE, qualified.type, null);
			return new BaseType(TypeKind.ERROR, qualified.posn);
		}
		
		// Get member
		IdentificationTable cdTable = table.getScope(qualClassDecl);
		MemberDecl md = (MemberDecl) cdTable.getDeclarationAtScope(ref.id.spelling);
				
		// Check the member exists at all
		if(md == null) {
			Reporter.report(ErrorType.UNDEFINED, ref.id, null);
			return new BaseType(TypeKind.ERROR, ref.id.posn);
		}
				
		// If the qualifed ref is a class declaration, members must be static
		else if(qualified instanceof ClassDecl) {
			if(!md.isStatic) {
				Reporter.report(ErrorType.STATIC, md, ref.id);
				return new BaseType(TypeKind.ERROR, ref.id.posn);
			} else if(md.isPrivate) {
				Reporter.report(ErrorType.VISIBILITY, md, ref.id);
				return new BaseType(TypeKind.ERROR, ref.id.posn);
			}
		}
		
		// The member should not be a method, as this is unsupported
		else if(qualified instanceof MethodDecl) {
			Reporter.report(ErrorType.UNDEFINED, ref.id, null);
		}
				
		// Otherwise, we can assume the object is a variable and attempt to access members
		else if(md.isPrivate && currentClassDecl != qualClassDecl) {
			Reporter.report(ErrorType.VISIBILITY, md, ref.id);
			return new BaseType(TypeKind.ERROR, ref.id.posn);
		}
		
		ref.id.decl = md;
		ref.decl = md;
		return md.type;
	}

	public Type visitIndexedRef(IndexedRef ref, IdentificationTable arg) {
				
		Type refType = ref.ref.visit(this, arg);

		// Make sure index is an integer
		Type indexExprType = ref.indexExpr.visit(this, arg);
		if (indexExprType.typeKind != TypeKind.INT) {
			Reporter.report(ErrorType.INVALID_INDEX, ref.indexExpr, null);
		}

		ref.decl = ref.ref.decl;
		return refType;
	}

	public Type visitIdRef(IdRef ref, IdentificationTable arg) {
		Type idType = ref.id.visit(this, arg);
		ref.decl = ref.id.decl;

		return idType;
	}

	public Type visitThisRef(ThisRef ref, IdentificationTable arg) {
		ref.decl = currentClassDecl;
		if(currentMethodDecl.isStatic) {
			Reporter.report(ErrorType.THIS, ref, currentMethodDecl);
		}
		
		return ref.decl.type;
	}
	

	// /////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	// /////////////////////////////////////////////////////////////////////////////

	public Type visitIdentifier(Identifier id, IdentificationTable arg) {

		// Check if identifier can be found in current scope
		Declaration decl = arg.getDeclarationAtScope(id.spelling);
		if (decl != null) { id.decl = decl; return decl.type; }
		
		// Access member and check visibility properties
		Declaration d = arg.getDeclaration(id.spelling);
		if(d == null) {
			Reporter.report(ErrorType.UNDEFINED, id, null);
			return new BaseType(TypeKind.ERROR, id.posn);
		} else {
			
			// Can only be a member at this point
			if(d.type.typeKind != TypeKind.CLASS) {
				MemberDecl md = (MemberDecl) d;
				
				// A static method cannot access instance members
				if(currentMethodDecl.isStatic && !md.isStatic) {
					Reporter.report(ErrorType.STATIC, md, id);
					return new BaseType(TypeKind.ERROR, id.posn);
				}
				
				// If a member declaration is private, it must be a member of the current class
				else if(md.isPrivate) {
					
					// Check if member is part of the current class
					IdentificationTable cdTable = table.getScope(currentClassDecl);
					if(cdTable.getDeclarationAtScope(md.name) == null) {
						Reporter.report(ErrorType.VISIBILITY, md, id);
						return new BaseType(TypeKind.ERROR, id.posn);
					}
				}
			}
			
			id.decl = d;
			return d.type;
		}
	}

	public Type visitOperator(Operator op, IdentificationTable arg) {
		
		switch (op.token.spelling) {
			case "!":
			case "&&":
			case "||":
				return new BaseType(TypeKind.BOOLEAN, op.posn);
				
			case ">":
			case "<":
			case "<=":
			case ">=":
				return new BaseType(TypeKind.RELATIONAL, op.posn);
				
			case "+":
			case "-":
			case "*":
			case "/":
				return new BaseType(TypeKind.INT, op.posn);
				
			case "==":
			case "!=":
				return new BaseType(TypeKind.EQUALS, op.posn);
		}

		return null;
	}

	public Type visitIntLiteral(IntLiteral num, IdentificationTable arg) {

		return new BaseType(TypeKind.INT, num.posn);
	}

	public Type visitBooleanLiteral(BooleanLiteral bool, IdentificationTable arg) {

		return new BaseType(TypeKind.BOOLEAN, bool.posn);
	}

}
