package miniJava.CodeGenerator;

import java.util.HashMap;

import mJAM.Machine;
import mJAM.Machine.*;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalyzer.Analyzer;

public class Encoder implements Visitor<Integer, Object> {
	
	// Keeps track of variables placed on the stack
	private int methodDataOffset = 0;
	
	// Maintains data to correct once completed traversal
	private HashMap<Integer, Code> patches = new HashMap<>();
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Object visitPackage(Package prog, Integer arg) {
		
		Machine.initCodeGen();
		
		// Initialize all static fields
		int staticFieldsSize = 0;
		for(ClassDecl cd : prog.classDeclList) {
			for(int i = 0; i < cd.fieldDeclList.size(); i++) {
				FieldDecl fd = cd.fieldDeclList.get(i);
				if(fd.isStatic) {
					int size = getSize(fd.type);
					fd.entity = new RuntimeEntity(size, staticFieldsSize, Reg.SB);
					staticFieldsSize += size;
				}
			}
		}
		
		if(staticFieldsSize > 0) {
			Machine.emit(Op.PUSH, staticFieldsSize);
		}
		
		// Build Classes
		for(ClassDecl cd : prog.classDeclList) {
			int cdPatch = Machine.nextInstrAddr();
			Machine.emit(Op.JUMP, Reg.CB, 0);
			cd.visit(this, cdPatch);
		}
		
		// Build main function
		methodDataOffset = Machine.linkDataSize;
		int mainPatch = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, 0);
		Analyzer.main.visit(this, null);

		// Run main function
		int mainLabel = Machine.nextInstrAddr();
		RuntimeEntity main = Analyzer.main.entity;

		Machine.emit(Op.LOADL, Machine.nullRep);
		Machine.emit(Op.CALL, Reg.CB, main.addr);
		Machine.emit(Machine.Op.HALT, 0 ,0, 0);
		Machine.patch(mainPatch, mainLabel);
		
		// Patch up
		for(Integer instr : patches.keySet()) {
			Code c = patches.get(instr);
			c.modify(instr);
		}
		
		return null;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Object visitClassDecl(ClassDecl cd, Integer patch) {
		
		// Get size of class
		int size = 0;
		for(FieldDecl fd : cd.fieldDeclList) {
			if(!fd.isStatic) {
				fd.visit(this, size);
				size += fd.entity.size;
			}
		}		
		
		// Build Instance Methods
		for(MethodDecl md : cd.methodDeclList) {
			if(md != Analyzer.main) {
				md.visit(this, null);
			}
		}
		
		// Build Class Descriptor
		int addr = Machine.nextInstrAddr();
		Machine.patch(patch, addr);
		
		Machine.emit(Op.LOADL, -1); 			// No superclasses allowed
		Machine.emit(Op.LOADL, size);			// Size of class
		Machine.emit(Op.LOADA, Reg.CB, addr);	// Address of class
		
		// Save entity
		cd.entity = new RuntimeEntity(size, addr, Reg.CB);
		
		return null;
	}

	@Override
	public Object visitFieldDecl(FieldDecl fd, Integer offset) {
		
		// Only non-static fields should ever reach this method
		int size = getSize(fd.type);
		fd.entity = new RuntimeEntity(size, offset, Reg.OB);
				
		return null;
	}

	@Override
	public Object visitMethodDecl(MethodDecl md, Integer arg) {
		
		// Reset for next local LB
		methodDataOffset = Machine.linkDataSize;
		
		// Save Entity
		int size = getSize(md.type);
		int addr = Machine.nextInstrAddr();
		md.entity = new RuntimeEntity(size, addr, Reg.CB);
		
		// Setup parameters
		int parameterSize = 0;
		for(ParameterDecl pd : md.parameterDeclList) {
			parameterSize += getSize(pd.type);
			pd.visit(this, -parameterSize);
		}
		
		// Build Body
		for(Statement s : md.statementList) {
			s.visit(this, null);
		}
		
		// Setup Return					
		if(md.returnExp != null) {
			md.returnExp.visit(this, null);
		}
		
		Machine.emit(Op.RETURN, size, 0, parameterSize);

		return null;
	}

	@Override
	public Object visitParameterDecl(ParameterDecl pd, Integer offset) {
		
		// Builds runtime entity (should be negative of LB)
		int size = getSize(pd.type);
		pd.entity = new RuntimeEntity(size, offset, Reg.LB);
				
		return null;
	}

	@Override
	public Object visitVarDecl(VarDecl decl, Integer arg) {

		// Builds runtime entity, offset at LB
		int size = getSize(decl.type);
		decl.entity = new RuntimeEntity(size, methodDataOffset, Reg.LB);
		
		// Allocates space on stack
		Machine.emit(Op.PUSH, size);
		methodDataOffset += size;
				
		return null;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Object visitBaseType(BaseType type, Integer arg) {
		
		return null;
	}

	@Override
	public Object visitClassType(ClassType type, Integer arg) {
		
		return null;
	}

	@Override
	public Object visitArrayType(ArrayType type, Integer arg) {
		
		return null;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Object visitBlockStmt(BlockStmt stmt, Integer arg) {
		
		// Execute statements
		int size = 0;
		for(Statement s : stmt.sl) {
			s.visit(this, null);
			if(s instanceof VarDeclStmt) {
				VarDeclStmt vds = (VarDeclStmt) s;
				size += vds.varDecl.entity.size;
			}
		}
		
		// Pop off variable declarations
		if(size > 0) {
			Machine.emit(Op.POP, size);
			methodDataOffset -= size;
		}
				
		return null;
	}

	@Override
	public Object visitVardeclStmt(VarDeclStmt stmt, Integer arg) {
		
		stmt.varDecl.visit(this, null);
		stmt.initExp.visit(this, null);
		
		RuntimeEntity e = stmt.varDecl.entity;
		Machine.emit(Op.STORE, e.size, e.reg, e.addr);
		
		return null;
	}

	@Override
	public Object visitAssignStmt(AssignStmt stmt, Integer arg) {
		
		// Can potentially reach declaration directly
		if(stmt.ref instanceof QualifiedRef) {
			
			QualifiedRef ref = (QualifiedRef) stmt.ref;
			MemberDecl md = (MemberDecl) ref.id.decl;
			
			// Just access directly
			if(md.isStatic) {
				stmt.val.visit(this, null);
				patches.put(Machine.nextInstrAddr(), new Code(md, true));
				Machine.emit(Op.STORE, getSize(md.type), Reg.SB, 0);
				
				return null;
			} 
			
			// Access member directly
			else if(ref.ref instanceof ThisRef) {
				stmt.val.visit(this, null);
				
				int addr = Machine.nextInstrAddr();
				int size = getSize(ref.id.decl.type);
				patches.put(addr, new Code(ref.id.decl, true));
				Machine.emit(Op.STORE, size, Reg.OB, 0);
				
				return null;
			}
		}

		// Must access member iteratively
		stmt.ref.visit(this, 1);
		stmt.val.visit(this, null);
		
		if(stmt.ref instanceof QualifiedRef) {
			Machine.emit(Prim.fieldupd);
		}
		
		else if(stmt.ref instanceof IndexedRef) {
			Machine.emit(Prim.arrayupd);
		}
		
		else if(stmt.ref instanceof IdRef) {
			int addr = Machine.nextInstrAddr();
			int size = getSize(stmt.ref.decl.type);
			patches.put(addr, new Code(stmt.ref.decl, true));
			Machine.emit(Op.STORE, size, Reg.ZR, 0);
		}
		
		return null;
	}

	@Override
	public Object visitCallStmt(CallStmt stmt, Integer arg) {
		
		MethodDecl md = (MethodDecl) stmt.methodRef.decl;
		
		// Request to print out
		if(md == Analyzer.println) {
			stmt.argList.get(0).visit(this, null);
			Machine.emit(Prim.putintnl);
			return null;
		}
		
		// Push parameters on (must iterate in reverse order)
		for(int i = stmt.argList.size() - 1; i >= 0; i--) {
			stmt.argList.get(i).visit(this, null);
		}
		
		// Call method directly
		if(md.isStatic) {
			patches.put(Machine.nextInstrAddr(), new Code(md, true));
			Machine.emit(Op.CALL, Reg.CB, 0);
		} 
		
		// Get address of qualified object
		else {
			if(stmt.methodRef instanceof QualifiedRef) {
				QualifiedRef ref = (QualifiedRef) stmt.methodRef;
				ref.ref.visit(this, null);
			} else {
				Machine.emit(Op.LOADA, Machine.addressSize, Reg.OB, 0);
			}
			
			patches.put(Machine.nextInstrAddr(), new Code(md, true));
			Machine.emit(Op.CALLI, Reg.CB, 0);
		}
		
		// Clear off stack if necessary
		int returnSize = getSize(md.type);
		if(returnSize > 0) {
			Machine.emit(Op.POP, returnSize);
		}
		
		return null;
	}

	@Override
	public Object visitIfStmt(IfStmt stmt, Integer arg) {
		
		stmt.cond.visit(this, null);
		
		// Do not have to build as many jump instructions in this case
		if(stmt.elseStmt == null) {
			int ifPatch = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, 0);
			
			stmt.thenStmt.visit(this, null);
			Machine.patch(ifPatch, Machine.nextInstrAddr());
			
			return null;
		}
		
		// Must jump out at end of 'if' clause
		int ifPatch = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, 0);
		
		stmt.thenStmt.visit(this, null);
		int thenPatch = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP, Reg.CB, 0);
		
		// Build 'else' clause
		Machine.patch(ifPatch, Machine.nextInstrAddr());
		stmt.elseStmt.visit(this, null);
		Machine.patch(thenPatch, Machine.nextInstrAddr());
		
		return null;
	}

	@Override
	public Object visitWhileStmt(WhileStmt stmt, Integer arg) {
		
		// Must check the condition each loop
		int whileLabel = Machine.nextInstrAddr();
		stmt.cond.visit(this, null);
		
		// Jump out once condition fails
		int whileEndPatch = Machine.nextInstrAddr();
		Machine.emit(Op.JUMPIF, Machine.falseRep, Reg.CB, 0);
		
		// Execute
		stmt.body.visit(this, null);
		Machine.emit(Op.JUMP, Reg.CB, whileLabel);
		Machine.patch(whileEndPatch, Machine.nextInstrAddr());
		
		return null;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Object visitUnaryExpr(UnaryExpr expr, Integer arg) {
		
		expr.expr.visit(this, null);
		switch(expr.operator.spelling) {
			case "!":
				Machine.emit(Prim.not);
				break;
			case "-":
				Machine.emit(Prim.neg);
				break;
		}
		
		return null;
	}

	@Override
	public Object visitBinaryExpr(BinaryExpr expr, Integer arg) {
		
		String op = expr.operator.spelling;
		
		if(op.equals("&&") || op.equals("||")) {
			int rep = (op.equals("&&")) ? Machine.falseRep : Machine.trueRep;
			
			expr.left.visit(this, null);
			int leftJump = Machine.nextInstrAddr();
			Machine.emit(Op.JUMPIF, rep, Reg.CB, 0);
			
			expr.right.visit(this, null);
			expr.operator.visit(this, null);
			
			Machine.patch(leftJump, Machine.nextInstrAddr());
		}
		
		else {
			expr.left.visit(this, null);
			expr.right.visit(this, null);
			expr.operator.visit(this, null);
		}
		
		return null;
	}

	@Override
	public Object visitRefExpr(RefExpr expr, Integer arg) {

		expr.ref.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitCallExpr(CallExpr expr, Integer arg) {
		
		MethodDecl md = (MethodDecl) expr.functionRef.decl;
		
		// Push parameters on (must iterate in reverse order)
		for(int i = expr.argList.size() - 1; i >= 0; i--) {
			expr.argList.get(i).visit(this, null);
		}
		
		// Call Method
		if(md.isStatic) {
			patches.put(Machine.nextInstrAddr(), new Code(md, true));
			Machine.emit(Op.CALL, Reg.CB, 0);
		} else {
			
			if(expr.functionRef instanceof QualifiedRef) {
				QualifiedRef ref = (QualifiedRef) expr.functionRef;
				ref.ref.visit(this, null);
			} else {
				Machine.emit(Op.LOADA, Machine.addressSize, Reg.OB, 0);
			}
			
			patches.put(Machine.nextInstrAddr(), new Code(md, true));
			Machine.emit(Op.CALLI, Reg.CB, 0);
		}
		
		return null;
	}

	@Override
	public Object visitLiteralExpr(LiteralExpr expr, Integer arg) {
		
		expr.literal.visit(this, null);
		
		return null;
	}

	@Override
	public Object visitNewObjectExpr(NewObjectExpr expr, Integer arg) {
		
		Declaration decl = expr.classtype.className.decl;
		
		patches.put(Machine.nextInstrAddr(), new Code(decl, true));
		Machine.emit(Op.LOADA, Reg.CB, 0);
		
		patches.put(Machine.nextInstrAddr(), new Code(decl, false));
		Machine.emit(Op.LOADL, 0);
		
		Machine.emit(Prim.newobj);
		
		return null;
	}

	@Override
	public Object visitNewArrayExpr(NewArrayExpr expr, Integer arg) {
				
		expr.sizeExpr.visit(this, null);
		Machine.emit(Prim.newarr);
				
		return null;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Object visitQualifiedRef(QualifiedRef ref, Integer arg) {
		
		// Array type always returns value
		if(ref.ref.decl.type.typeKind == TypeKind.ARRAY) {
			
			// Get address of object
			if(ref.ref instanceof QualifiedRef) {
				ref.ref.visit(this, null);
			} else if(ref.ref instanceof ThisRef) {
				patches.put(Machine.nextInstrAddr(), new Code(ref.id.decl, true));
				Machine.emit(Op.STORE, getSize(ref.id.decl.type), Reg.OB, 0);
			} else {
				patches.put(Machine.nextInstrAddr(), new Code(ref.ref.decl, true));
				Machine.emit(Op.LOAD, Machine.addressSize, Reg.LB, 0);
			}
			
			Machine.emit(Op.LOADL, 1);
			Machine.emit(Prim.sub);
			Machine.emit(Op.LOADI, Machine.integerSize);
			
			return null;
		}
		
		MemberDecl md = (MemberDecl) ref.id.decl;
		
		// Assigning
		if(arg != null) {
			ref.ref.visit(this, null);
			patches.put(Machine.nextInstrAddr(), new Code(ref.id.decl, true));
			Machine.emit(Op.LOADL, 0);
		}
		
		// Retrieving
		else if(md.isStatic) {
			patches.put(Machine.nextInstrAddr(), new Code(md, true));
			Machine.emit(Op.LOAD, getSize(md.type), Reg.SB, 0);
		} else {
			ref.ref.visit(this, null);
			int addr = Machine.nextInstrAddr();
			patches.put(addr, new Code(ref.decl, true));
			
			Machine.emit(Op.LOADL, 0);
			Machine.emit(Prim.fieldref);
		}	
		
		return null;
	}

	@Override
	public Object visitIndexedRef(IndexedRef ref, Integer arg) {
		
		ref.ref.visit(this, null);
		ref.indexExpr.visit(this, null);
		
		// Retrieving
		if(arg == null) {
			Machine.emit(Prim.arrayref);
		}
		
		return null;
	}

	@Override
	public Object visitIdRef(IdRef ref, Integer arg) {
		
		ref.entity = ref.decl.entity;
		
		// Retrieving
		if(arg == null) {
			int size = getSize(ref.decl.type);
			int addr = Machine.nextInstrAddr();
			patches.put(addr, new Code(ref.decl, true));
			
			Machine.emit(Op.LOAD, size, Reg.ZR, 0);
		}
		
		return null;
	}

	@Override
	public Object visitThisRef(ThisRef ref, Integer arg) {
		
		Machine.emit(Op.LOADA, Machine.addressSize, Reg.OB, 0);
		
		return null;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Object visitIdentifier(Identifier id, Integer arg) {
		
		return null;
	}

	@Override
	public Object visitOperator(Operator op, Integer arg) {
		
		switch(op.spelling) {
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
		
		return null;
	}

	@Override
	public Object visitIntLiteral(IntLiteral num, Integer arg) {
		
		Integer lit = Integer.parseInt(num.spelling);
		Machine.emit(Op.LOADL, lit.intValue());
		
		return null;
	}

	@Override
	public Object visitBooleanLiteral(BooleanLiteral bool, Integer arg) {
		
		if(bool.spelling.equals("true")) {
			Machine.emit(Op.LOADL, Machine.trueRep);
		} else {
			Machine.emit(Op.LOADL, Machine.falseRep);
		}
		
		return null;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// Convenience Methods
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

}
