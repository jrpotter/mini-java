package miniJava.CodeGenerator;

import mJAM.Machine;
import mJAM.Machine.*;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.Analyzer;

public class Encoder implements Visitor<Integer, Integer> {
	
	private int mdLBOffset = 0;

	// /////////////////////////////////////////////////////////////////////////////
	//
	// Convenience Functions
	//
	// /////////////////////////////////////////////////////////////////////////////	
	
	/**
	 * Get size of type for declaration purposes.
	 * @param t
	 * @return
	 */
	private int getSize(Type t) {
		switch(t.typeKind) {
			case ARRAY:
			case CLASS:
				return Machine.addressSize;
			case INT:
				return Machine.integerSize;
			case BOOLEAN:
				return Machine.booleanSize;
			case VOID:
				return 0;
			default:
				return -1;
		}
	}

	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	// /////////////////////////////////////////////////////////////////////////////
		
	@Override
	public Integer visitPackage(Package prog, Integer arg) {
		
		// Initialize static fields
		for(ClassDecl cd : prog.classDeclList) {
			for(int i = 0; i < cd.fieldDeclList.size(); i++) {
				if(cd.fieldDeclList.get(i).isStatic) {
					cd.fieldDeclList.get(i).visit(this, i);
				}
			}
		}
		
		int patch = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, 0);
		
		// Initialize all classes and methods
		for(ClassDecl cd : prog.classDeclList) {
			cd.visit(this, 1);
		}
		
		// Link classes & build stack
		Machine.patch(patch, Machine.nextInstrAddr());
		for(ClassDecl cd : prog.classDeclList) {
			int cdPatch = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, 0);
			Machine.patch(cdPatch, cd.visit(this, 0));
		}
		
		// Build main function
		mdLBOffset = Machine.linkDataSize;
		int mainPatch = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, 0);
		Analyzer.mainMethod.visit(this, 0);
		
		
		// Run main function
		int mainLabel = Machine.nextInstrAddr();
		RuntimeEntity main = Analyzer.mainMethod.entity;
		
		Machine.emit(Op.LOADL, Machine.nullRep);
		Machine.emit(Op.CALL, Reg.CB, main.addr);
		Machine.emit(Machine.Op.HALT, 0 ,0, 0);
		Machine.patch(mainPatch, mainLabel);
		
		return 0;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitClassDecl(ClassDecl cd, Integer init) {
		
		/* Give an address to each method and class, so it can be referred
		 * to directly by jumping.
		*/
		if(init != 0) {
			
			// Get size of class
			int size = 0;
			for(FieldDecl fd : cd.fieldDeclList) {
				if(!fd.isStatic) {
					fd.visit(this, size);
					size += getSize(fd.type);
				}
			}
			
			// Build Methods
			for(MethodDecl md : cd.methodDeclList) {
				md.visit(this, init);
			}
			
			// Runtime Entity
			int addr = Machine.nextInstrAddr();
			cd.entity = new RuntimeEntity(size, addr, Reg.CB, addr);
			Machine.emit(Op.JUMP, Reg.CB, 0);
			
			return addr;
		} 
		
		// Build Instance Methods
		for(MethodDecl md : cd.methodDeclList) {
			if(!md.isStatic) {
				md.visit(this, 0);
			}
		}
		
		// Build class descriptor
		int label = Machine.nextInstrAddr();
		Machine.patch(cd.entity.instr, label);
		
		Machine.emit(Op.LOADL, -1); 					// No superclasses allowed
		Machine.emit(Op.LOADL, cd.entity.size);			// Size of class
		Machine.emit(Op.LOADA, Reg.CB, cd.entity.addr);	// Address of class
		
		return label;
	}

	@Override
	public Integer visitFieldDecl(FieldDecl fd, Integer addr) {

		int size = getSize(fd.type);
		
		// Static fields are placed onto the stack
		if(fd.isStatic) {
			fd.entity = new RuntimeEntity(size, addr, Reg.SB);
			Machine.emit(Op.PUSH, size);
		} else {
			fd.entity = new RuntimeEntity(size, addr, Reg.OB);
		}

		return 0;
	}

	
	@Override
	public Integer visitMethodDecl(MethodDecl md, Integer init) {
		
		mdLBOffset = Machine.linkDataSize;
		
		if(init != 0) {
			int size = getSize(md.type);
			int addr = Machine.nextInstrAddr();
			md.entity = new RuntimeEntity(size, addr, Reg.CB, addr);
			Machine.emit(Op.JUMP, Reg.CB, 0);
		} 
		
		else {
			// Setup parameters
			int paramS = 0;
			for(ParameterDecl pd : md.parameterDeclList) {
				paramS += getSize(md.type);
				pd.visit(this, -paramS);
			}
			
			// Setup body
			int addr = Machine.nextInstrAddr();
			Machine.patch(md.entity.instr, addr);
			for(Statement s : md.statementList) {
				s.visit(this, 0);
			}
			
			// Setup return statement
			if(md.returnExp != null) {
				md.returnExp.visit(this, 0);
			}
			
			// Return from function
			// Machine.emit(Op.HALT, 4, 0, 0); (See snapshot)
			Machine.emit(Op.RETURN, md.entity.size, 0, paramS);
		}
		
		return 0;
	}

	/**
	 * Note parameter addresses are negative to LB since they are
	 * placed onto the stack before emitting a CALL.
	 */
	@Override
	public Integer visitParameterDecl(ParameterDecl pd, Integer addr) {
		
		// Builds runtime entity, offset at LB
		int size = getSize(pd.type);
		pd.entity = new RuntimeEntity(size, addr, Reg.LB);
		
		return 0;
	}

	/**
	 * Variable declarations are never disposed (even in static closures).
	 */
	@Override
	public Integer visitVarDecl(VarDecl decl, Integer addr) {
		
		// Builds runtime entity, offset at LB
		int size = getSize(decl.type);
		decl.entity = new RuntimeEntity(size, addr, Reg.LB);
		
		// Allocates space on stack
		Machine.emit(Op.PUSH, size);
		
		return 0;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	// 
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitBaseType(BaseType type, Integer arg) {
		
		return 0;
	}

	@Override
	public Integer visitClassType(ClassType type, Integer arg) {
		
		return 0;
	}

	@Override
	public Integer visitArrayType(ArrayType type, Integer arg) {
		
		return 0;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	// /////////////////////////////////////////////////////////////////////////////

	/**
	 * We note a block statement may contain a variable declaration,
	 * that should go out of scope at the end of the statement.
	 * If the passed argument is 1, we indicate we want this to
	 * happen.
	 */
	@Override
	public Integer visitBlockStmt(BlockStmt stmt, Integer arg) {
		
		// Execute statements
		int size = 0;
		for(Statement s : stmt.sl) {
			s.visit(this, 0);
			
			// Push variable declarations if necessary
			if(arg == 1 && s instanceof VarDeclStmt) {
				VarDeclStmt decl = (VarDeclStmt) s;
				size += getSize(decl.varDecl.type);
			}
		}
		
		// Pop off variable declarations
		Machine.emit(Op.POP, size);
		
		return 0;
	}

	/**
	 * We declare the variable, place the RHS onto the stack, and
	 * replace the value of the variable with the top of the stack.
	 */
	@Override
	public Integer visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
		stmt.varDecl.visit(this, mdLBOffset);
		stmt.initExp.visit(this, 0);
		
		// Assign value
		RuntimeEntity e = stmt.varDecl.entity;
		Machine.emit(Op.STORE, e.size, e.register, e.addr);
		
		// Update position
		mdLBOffset += getSize(stmt.varDecl.type);
		
		return 0;
	}

	@Override
	public Integer visitAssignStmt(AssignStmt stmt, Integer arg) {
		
		stmt.ref.visit(this, 0);
		
		// Setup
		RuntimeEntity e = stmt.ref.entity;
		
		// Build code accordingly
		if(stmt.ref instanceof QualifiedRef) {			
			e.parent.load();
			Machine.emit(Op.LOADL, e.addr);
			stmt.val.visit(this, 0);
			Machine.emit(Prim.fieldupd);
		}
		
		else if(stmt.ref instanceof IndexedRef) {
			IndexedRef ref = (IndexedRef) stmt.ref;
			e.load();
			ref.indexExpr.visit(this, 0);
			stmt.val.visit(this, 0);
			Machine.emit(Prim.arrayupd);
		}
		
		else if(stmt.ref instanceof IdRef) {
			stmt.val.visit(this, 0);
			Machine.emit(Op.STORE, e.size, e.register, e.addr);
		}
		
		return 0;
	}

	/**
	 * The method in question can either be an instance or static
	 * function (it must be a MethodDecl).
	 */
	@Override
	public Integer visitCallStmt(CallStmt stmt, Integer arg) {
		
		MethodDecl md = (MethodDecl) stmt.methodRef.decl;
		
		// Request to print out
		if(md == Analyzer.println) {
			stmt.argList.get(0).visit(this, 0);
			Machine.emit(Prim.putintnl);
			return 0;
		}
		
		// Push parameters on (must iterate in reverse order)
		for(int i = stmt.argList.size() - 1; i >= 0; i--) {
			stmt.argList.get(i).visit(this, 0);
		}
		
		// Call Method
		stmt.methodRef.visit(this, 0);
		if(md.isStatic) {
			Machine.emit(Op.CALL, Reg.CB, md.entity.addr);
		} else {
			stmt.methodRef.entity.call();
			Machine.emit(Op.CALLI, Reg.CB, md.entity.addr);
		}
		
		return 0;
	}

	@Override
	public Integer visitIfStmt(IfStmt stmt, Integer arg) {
		
		stmt.cond.visit(this, 0);
		
		// Build Then Statement
		int ifPatch = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, Machine.trueRep, Reg.CB, 0);
		
		int elsePatch = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, 0);
		
		int thenLabel = Machine.nextInstrAddr();
		stmt.thenStmt.visit(this, 0);
		
		int thenPatch = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, 0);
		
		// Connect labels/patches
		int endLabel = Machine.nextInstrAddr();
		Machine.patch(elsePatch, endLabel);
		
		if(stmt.elseStmt != null) {
			stmt.elseStmt.visit(this, 0);
			endLabel = Machine.nextInstrAddr();
		}
		
		Machine.patch(ifPatch, thenLabel);
		Machine.patch(thenPatch, endLabel);

		return 0;
	}

	/**
	 * We note since the same declaration can be reached multiple times, we
	 * are forced to pop each declaration initialized.
	 */
	@Override
	public Integer visitWhileStmt(WhileStmt stmt, Integer arg) {
		
		// Must check the condition each loop
		int whileLabel = Machine.nextInstrAddr();
		stmt.cond.visit(this, 0);
		
		// Jump out once condition fails
		int whileEndPatch = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, 0);
		
		// Execute
		stmt.body.visit(this, 1);
		Machine.emit(Op.JUMP, Reg.CB, whileLabel);
		Machine.patch(whileEndPatch, Machine.nextInstrAddr());

		return 0;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitUnaryExpr(UnaryExpr expr, Integer arg) {
		
		expr.expr.visit(this, 0);
		switch(expr.operator.spelling) {
			case "!":
				Machine.emit(Prim.not);
				break;
			case "-":
				Machine.emit(Prim.neg);
				break;
		}
		
		return 0;
	}

	@Override
	public Integer visitBinaryExpr(BinaryExpr expr, Integer arg) {
		expr.left.visit(this, 0);
		expr.right.visit(this, 0);
		expr.operator.visit(this, 0);
		
		return 0;
	}

	@Override
	public Integer visitRefExpr(RefExpr expr, Integer arg) {

		expr.ref.visit(this, 0);
		
		// Build code accordingly
		if(expr.ref instanceof QualifiedRef) {
			
			// Must be requesting length of array
			if(expr.ref.decl.type.typeKind == TypeKind.ARRAY) {
				int size = Machine.integerSize;
				int addr = expr.ref.entity.addr + size;
				Machine.emit(Op.LOAD, size, Reg.LB, addr);
			} else {
				expr.ref.entity.load();
			}
		}
		
		else if(expr.ref instanceof IndexedRef) {
			IndexedRef ref = (IndexedRef) expr.ref;
			ref.entity.load();
			ref.indexExpr.visit(this, 0);
			Machine.emit(Prim.arrayref);
		} 
		
		else if(expr.ref instanceof ThisRef) {
			RuntimeEntity e = expr.ref.entity;
			Machine.emit(Op.LOADA, e.size, e.register, e.addr);
		}
		
		else {
			expr.ref.entity.load();
		}
		
		return 0;
	}

	@Override
	public Integer visitCallExpr(CallExpr expr, Integer arg) {
		
		// Push parameters on (must iterate in reverse order)
		for(int i = expr.argList.size() - 1; i >= 0; i--) {
			expr.argList.get(i).visit(this, 0);
		}
						
		// Call method
		MethodDecl md = (MethodDecl) expr.functionRef.decl;
		expr.functionRef.visit(this, 0);
		if(md.isStatic) {
			Machine.emit(Op.CALL, Reg.CB, md.entity.addr);
		} else {
			expr.functionRef.entity.call();
			Machine.emit(Op.CALLI, Reg.CB, md.entity.addr);
		}
		
		return 0;
	}

	@Override
	public Integer visitLiteralExpr(LiteralExpr expr, Integer arg) {
		
		expr.literal.visit(this, 0);
		
		return 0;
	}

	@Override
	public Integer visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
	
		RuntimeEntity e = expr.classtype.className.decl.entity;
		Machine.emit(Op.LOADA, e.register, e.addr);
		Machine.emit(Op.LOADL, e.size);
		Machine.emit(Prim.newobj);
		
		return 0;
	}

	/**
	 * Returns the address where the new array's size is being
	 * stored (as it cannot be easily accessed otherwise).
	 */
	@Override
	public Integer visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
		
		// Setup
		int addr = mdLBOffset;
		int size = Machine.integerSize;
		mdLBOffset += Machine.integerSize;
		
		// Add to stack
		expr.sizeExpr.visit(this, 0);
		
		// Create new array
		Machine.emit(Op.LOAD, size, Reg.LB, addr);
		Machine.emit(Prim.newarr);
				
		return addr;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitQualifiedRef(QualifiedRef ref, Integer arg) {
		
		ref.ref.visit(this, 0);
		
		// Must be accessing length of an array
		if(ref.ref.decl.type.typeKind == TypeKind.ARRAY) {
			ref.decl = ref.ref.decl;
			ref.entity = ref.ref.entity;
		} 
		
		// Access class member
		else {
			ref.entity = ref.id.decl.entity;
			ref.entity.parent = ref.ref.entity;
		}
		
		return 0;
	}

	@Override
	public Integer visitIndexedRef(IndexedRef ref, Integer arg) {
		
		ref.entity = ref.decl.entity;
		
		return 0;
	}

	@Override
	public Integer visitIdRef(IdRef ref, Integer arg) {

		ref.entity = ref.decl.entity;
		
		return 0;
	}

	@Override
	public Integer visitThisRef(ThisRef ref, Integer arg) {
		
		RuntimeEntity e = ref.decl.entity;
		ref.entity = new RuntimeEntity(e.size, 0, Reg.OB);
		ref.entity.indirect = true;
		
		return 0;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Integer visitIdentifier(Identifier id, Integer arg) {
		
		return 0;
	}

	@Override
	public Integer visitOperator(Operator op, Integer arg) {
		
		switch(op.token.spelling) {
			case "+":
				Machine.emit(Prim.add);
				break;
			case "-":
				Machine.emit(Prim.sub);
				break;
			case "*":
				Machine.emit(Prim.mult);
				break;
			case "/":
				Machine.emit(Prim.div);
				break;
			case "<":
				Machine.emit(Prim.lt);
				break;
			case ">":
				Machine.emit(Prim.gt);
				break;
			case "<=":
				Machine.emit(Prim.le);
				break;
			case ">=":
				Machine.emit(Prim.ge);
				break;
			case "==":
				Machine.emit(Prim.eq);
				break;
			case "!=":
				Machine.emit(Prim.ne);
				break;
			case "&&":
				Machine.emit(Prim.and);
				break;
			case "||":
				Machine.emit(Prim.or);
				break;
		}
		
		return 0;
	}

	@Override
	public Integer visitIntLiteral(IntLiteral num, Integer arg) {
		
		Integer lit = Integer.parseInt(num.spelling);
		Machine.emit(Op.LOADL, lit.intValue());
		
		return 0;
	}

	@Override
	public Integer visitBooleanLiteral(BooleanLiteral bool, Integer arg) {
		
		if(bool.spelling.equals("true")) {
			Machine.emit(Op.LOADL, Machine.trueRep);
		} else {
			Machine.emit(Op.LOADL, Machine.falseRep);
		}
		
		return 0;
	}

}
