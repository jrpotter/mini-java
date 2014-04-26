package miniJava.CodeGenerator;

import mJAM.Machine;
import miniJava.AbstractSyntaxTrees.*;

public class Code {
	
	public boolean addr;
	public Declaration decl;
	
	/**
	 * If addr true, returns the address of the declaration.
	 * Otherwise, uses the size of the declaration in its place.
	 * @param op
	 * @param decl
	 * @param addr
	 */
	public Code(Declaration decl, boolean addr) {
		this.decl = decl;
		this.addr = addr;
	}
	
	/**
	 * 
	 * @param index
	 */
	public void modify(int instr) {

		// Setup size
		switch(decl.type.typeKind) {
			case ARRAY:
			case CLASS:
				Machine.code[instr].n = Machine.addressSize;
			case INT:
				Machine.code[instr].n = Machine.integerSize;
			case BOOLEAN:
				Machine.code[instr].n = Machine.booleanSize;
			case VOID:
				Machine.code[instr].n = 0;
			default:
				Machine.code[instr].n = -1;
		}
		
		// Setup displacement
		if(addr) {
			Machine.code[instr].d += decl.entity.addr;
		} else {
			Machine.code[instr].d += decl.entity.size;
		}
		
		// Setup register
		Machine.code[instr].r = decl.entity.reg.ordinal();
	}

}
