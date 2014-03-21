package miniJava.ContextualAnalyzer;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;
import miniJava.Exceptions.*;

// Check types
// Ensure static members
// Check for main function
public class Analyzer implements Visitor<IdentificationTable, Type> {
		
	// Topmost Identification Table
	private IdentificationTable table = new IdentificationTable();
	
	// Current class decl is for use with the 'this' keyword
	private Declaration currentClassDecl = null;
	
	/**
	 * Adds predefined names to identification table.
	 */
	public Analyzer() {		
		try {			
			String __PrintStream = "class _PrintStream { public void println(int n){} }";
			new Parser(new Scanner(__PrintStream)).parse().visit(this, table);
			
			String _System = "class System {  public static _PrintStream out; }";
			new Parser(new Scanner(_System)).parse().visit(this, table);
			
			String _String = "class String { }";
			new Parser(new Scanner(_String)).parse().visit(this, table);
			
		} catch(ParsingException e) {
			System.out.println("Predefined declarations parsing error!");
		} catch(ScanningException e) {
			System.out.println("Predefined declarations scanning error!");
		}
	}
	
	/**
	 * Check that two types match.
	 * @param t1
	 * @param t2
	 * @return
	 */
	private void match(Type t1, Type t2) {
		
		// Check class types match
		if(t1.typeKind == TypeKind.CLASS) {
			if(t2.typeKind != TypeKind.CLASS) {
				Reporter.report(ErrorType.TYPE_MISMATCH, t1, t2);
			} else {
				ClassType c1 = (ClassType) t1;
				ClassType c2 = (ClassType) t2;
				if(!c1.className.spelling.equals(c2.className.spelling)) {
					Reporter.report(ErrorType.TYPE_CLASS_MISMATCH, t1, t2);
				}
			}
		}
		
		// Check array types match
		else if(t1.typeKind == TypeKind.ARRAY) {
			if(t2.typeKind != TypeKind.ARRAY) {
				Reporter.report(ErrorType.TYPE_MISMATCH, t1, t2);
			} else {
				ArrayType a1 = (ArrayType) t1;
				ArrayType a2 = (ArrayType) t2;
				if(a1.eltType.typeKind != a2.eltType.typeKind) {
					Reporter.report(ErrorType.TYPE_ARRAY_MISMATCH, t1, t2);
				}
			}
		}
		
		// Check primitive types match
		else if(t1.typeKind != t2.typeKind) {
			Reporter.report(ErrorType.TYPE_MISMATCH, t1, t2);
		}
	}
		
	@Override
	public Type visitPackage(Package prog, IdentificationTable arg) {
		
		/* Since classes and static methods/fields can be referenced
		 * before the classes themselves are declared, we preprocess
		 * all classes and methods first.
		 */
		for(ClassDecl cd : prog.classDeclList) {
			
			IdentificationTable cdTable = table.openScope(cd);
			if(cdTable != null) {
				
				// Simply add fields to current scope
				for(FieldDecl fd : cd.fieldDeclList) {
					cdTable.setDeclarationAtScope(fd);
				}
				
				// Just Add Declaration (also creates another identification table)
				for(MethodDecl md : cd.methodDeclList) {
					cdTable.openScope(md);
				}
			}
		}
		
		// Begin Traversal
		for(ClassDecl cd : prog.classDeclList) {
			currentClassDecl = cd;
			cd.visit(this, table);
		}
		
		return null;
	}

	@Override
	public Type visitClassDecl(ClassDecl cd, IdentificationTable arg) {
		IdentificationTable cdTable = arg.getScope(cd);
		if(cdTable != null) {
			
			for(FieldDecl fd : cd.fieldDeclList) {
				fd.visit(this, cdTable);
			}
			
			for(MethodDecl md : cd.methodDeclList) {
				md.visit(this, cdTable);
			}
		}
		
		return cd.type;
	}

	@Override
	public Type visitFieldDecl(FieldDecl fd, IdentificationTable arg) {
		
		// Must check that the type of the field can be identified
		table.validateClassId(fd.type);
		
		return fd.type;
	}

	@Override
	public Type visitMethodDecl(MethodDecl md, IdentificationTable arg) {
		
		IdentificationTable mdTable = arg.getScope(md);
		
		// Must check that the type of the method can be identified
		table.validateClassId(md.type);
		
		// Continue Traversal
		if(mdTable != null) {
			for(ParameterDecl pd : md.parameterDeclList) {
				pd.visit(this, mdTable);
			}
			
			for(Statement s : md.statementList) {
				s.visit(this, mdTable);
			}
			
			// Check that return type matches expected type
			if(md.returnExp != null) {
				Type returnType = md.returnExp.visit(this, mdTable);
				match(md.type, returnType);
			} else if(md.type.typeKind != TypeKind.VOID) {
				Reporter.report(ErrorType.NO_RETURN_EXPRESSION, md.type);
			}
		}
		
		return md.type;
	}

	@Override
	public Type visitParameterDecl(ParameterDecl pd, IdentificationTable arg) {
		arg.setDeclarationAtScope(pd);
		table.validateClassId(pd.type);
		return pd.type;
	}

	@Override
	public Type visitVarDecl(VarDecl decl, IdentificationTable arg) {
		arg.setDeclarationAtScope(decl);
		table.validateClassId(decl.type);
		return decl.type;
	}

	@Override
	public Type visitBaseType(BaseType type, IdentificationTable arg) {
		
		return type;
	}

	@Override
	public Type visitClassType(ClassType type, IdentificationTable arg) {
		type.className.visit(this, arg);
		return type;
	}

	@Override
	public Type visitArrayType(ArrayType type, IdentificationTable arg) {
		type.eltType.visit(this, arg);
		return type;
	}

	@Override
	public Type visitBlockStmt(BlockStmt stmt, IdentificationTable arg) {
		arg.pushLevel();
		for(Statement s : stmt.sl) {
			s.visit(this, arg);
		}
		arg.popLevel();
		
		return null;
	}

	@Override
	public Type visitVardeclStmt(VarDeclStmt stmt, IdentificationTable arg) {
		
		// The stmt of the vardecl may not refer to the variable itself so we check the stmt first
		Type initExpType = stmt.initExp.visit(this, arg);
		Type varDeclType = stmt.varDecl.visit(this, arg);
		match(varDeclType, initExpType);

		return varDeclType;
	}

	@Override
	public Type visitAssignStmt(AssignStmt stmt, IdentificationTable arg) {
		Type valType = stmt.val.visit(this, arg);
		Type refType = stmt.ref.visit(this, arg);
		match(valType, refType);
		
		return refType;
	}

	@Override
	public Type visitCallStmt(CallStmt stmt, IdentificationTable arg) {
		Type methodType = stmt.methodRef.visit(this, arg);
		
		// Check that parameter count is correct and each type is correct
		MethodDecl decl = (MethodDecl) stmt.methodRef.decl;
		if(decl.parameterDeclList.size() != stmt.argList.size()) {
			Reporter.report(ErrorType.INVALID_PARAM_COUNT, stmt);
		} else {
			for(int i = 0; i < stmt.argList.size(); i++) {
				Type exprType = stmt.argList.get(i).visit(this, arg);
				Type pdType = decl.parameterDeclList.get(i).type;
				match(pdType, exprType);
			}
		}
		
		return methodType;
	}

	@Override
	public Type visitIfStmt(IfStmt stmt, IdentificationTable arg) {
		
		// The conditional statment must be a boolean
		Type condType = stmt.cond.visit(this, arg);
		match(new BaseType(TypeKind.BOOLEAN, null), condType);
		
		// A single vardecl cannot exist after a conditional statement
		if(stmt.thenStmt instanceof VarDeclStmt) {
			Reporter.report(ErrorType.VAR_COND_ONLY, stmt.thenStmt);
		} else {
			stmt.thenStmt.visit(this, arg);
			if(stmt.elseStmt != null) {
				if(stmt.elseStmt instanceof VarDeclStmt) {
					Reporter.report(ErrorType.VAR_COND_ONLY, stmt.elseStmt);
				} else {
					stmt.elseStmt.visit(this, arg);
				}
			}
		}
			
		return null;
	}

	@Override
	public Type visitWhileStmt(WhileStmt stmt, IdentificationTable arg) {
		
		// The conditional statment must be a boolean
		Type condType = stmt.cond.visit(this, arg);
		match(new BaseType(TypeKind.BOOLEAN, null), condType);
		
		if(stmt.body instanceof VarDeclStmt) {
			Reporter.report(ErrorType.VAR_COND_ONLY, stmt.body);
		} else {
			stmt.body.visit(this, arg);
		}
		
		return null;
	}

	@Override
	public Type visitUnaryExpr(UnaryExpr expr, IdentificationTable arg) {
		Type opType = expr.operator.visit(this, arg);
		Type exprType = expr.expr.visit(this, arg);
		match(opType, exprType);
		
		return null;
	}

	@Override
	public Type visitBinaryExpr(BinaryExpr expr, IdentificationTable arg) {
		Type opType = expr.operator.visit(this, arg);
		Type leftType = expr.left.visit(this, arg);
		Type rightType = expr.right.visit(this, arg);
		
		match(opType, leftType);
		match(opType, rightType);
		
		return null;
	}

	@Override
	public Type visitRefExpr(RefExpr expr, IdentificationTable arg) {
		
		return expr.ref.visit(this, arg);
	}

	@Override
	public Type visitCallExpr(CallExpr expr, IdentificationTable arg) {
		Type functionType = expr.functionRef.visit(this, arg);
		
		// Check that parameter count is correct and each type is correct
		MethodDecl decl = (MethodDecl) expr.functionRef.decl;
		if(decl.parameterDeclList.size() != expr.argList.size()) {
			Reporter.report(ErrorType.INVALID_PARAM_COUNT, expr);
		} else {
			for(int i = 0; i < expr.argList.size(); i++) {
				Type exprType = expr.argList.get(i).visit(this, arg);
				Type pdType = decl.parameterDeclList.get(i).type;
				match(pdType, exprType);
			}
		}
		
		return functionType;
	}

	@Override
	public Type visitLiteralExpr(LiteralExpr expr, IdentificationTable arg) {
		
		return expr.literal.visit(this, arg);
	}

	@Override
	public Type visitNewObjectExpr(NewObjectExpr expr, IdentificationTable arg) {
		
		return expr.classtype.visit(this, arg);
	}

	@Override
	public Type visitNewArrayExpr(NewArrayExpr expr, IdentificationTable arg) {
		Type sizeExprType = expr.sizeExpr.visit(this, arg);
		if(sizeExprType.typeKind != TypeKind.INT) {
			Reporter.report(ErrorType.INVALID_INDEX, expr.sizeExpr);
		}
		
		return expr.eltType.visit(this, arg);
	}

	@Override
	public Type visitQualifiedRef(QualifiedRef ref, IdentificationTable arg) {
		ref.ref.visit(this, arg);
		ref.id.visit(this, arg);
		
		// Check that each declaration is nested properly
		if(ref.decl == null || table.getDeclaration(ref.decl.name) == null) {
			System.out.println(ref.id.spelling);
			// report(ErrorType.MISSING_DECL, ref.ref.decl);
		}
		
		return null;
	}

	@Override
	public Type visitIndexedRef(IndexedRef ref, IdentificationTable arg) {
		ref.ref.visit(this, arg);
		ref.decl = ref.ref.decl;
		
		// Make sure index is an integer
		Type indexExprType = ref.indexExpr.visit(this, arg);
		if(indexExprType.typeKind != TypeKind.INT) {
			Reporter.report(ErrorType.INVALID_INDEX, ref.indexExpr);
		}
		
		return ref.decl.type;
	}

	@Override
	public Type visitIdRef(IdRef ref, IdentificationTable arg) {
		ref.id.visit(this, arg);
		ref.decl = ref.id.decl;
		
		return ref.decl.type;
	}

	@Override
	public Type visitThisRef(ThisRef ref, IdentificationTable arg) {
		ref.decl = currentClassDecl;
		
		return ref.decl.type;
	}

	@Override
	public Type visitIdentifier(Identifier id, IdentificationTable arg) {
		// Try to find identifier, reporting a message if it cannot be found
		Declaration decl = arg.getDeclaration(id.spelling);
		if(decl == null) {
			Reporter.report(ErrorType.UNIDENTIFIED, id);
			return new BaseType(TypeKind.ERROR, null);
		} else {
			id.decl = decl;
			return decl.type;
		}
	}

	@Override
	public Type visitOperator(Operator op, IdentificationTable arg) {
		
		switch(op.token.spelling) {
			case "!":
			case ">":
			case "<":
			case "==":
			case "<=":
			case ">=":
			case "!=":
			case "&&":
			case "||":
				return new BaseType(TypeKind.BOOLEAN, op.posn);
				
			case "+":
			case "-":
			case "*":
			case "/":
				return new BaseType(TypeKind.INT, op.posn);
				
			default:
				Reporter.report(ErrorType.UNIDENTIFIED_TYPE, op);
				return new BaseType(TypeKind.ERROR, op.posn);
		}
	}

	@Override
	public Type visitIntLiteral(IntLiteral num, IdentificationTable arg) {
		
		return new BaseType(TypeKind.INT, num.posn);
	}

	@Override
	public Type visitBooleanLiteral(BooleanLiteral bool, IdentificationTable arg) {
		
		return new BaseType(TypeKind.BOOLEAN, bool.posn);
	}

}
