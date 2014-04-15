package miniJava.CodeGenerator;

import mJAM.Machine;
import mJAM.Machine.Op;
import mJAM.Machine.Prim;
import mJAM.Machine.Reg;

public class RuntimeEntity {
	
	public int size = -1;				// Size of type
	public int addr = -1;				// Position relative to register
	public int instr = -1;				// Instruction relative to CB
	public Reg register = Reg.ZR;		// Register to offset by
	
	// For use with nested elements
	public boolean indirect = false;
	public RuntimeEntity parent = null;
	
	public RuntimeEntity(int size, int addr, Reg register) {
		this(size, addr, register, -1);
	}
	
	public RuntimeEntity(int size, int addr, Reg register, int instr) {
		this.size = size;
		this.addr = addr;
		this.register = register;
		this.instr = instr;
	}
	
	// Load entity into memory (if this, should call LOADA)
	public void load() {
		if(parent != null) {
			parent.load();
			Machine.emit(Op.LOADL, addr);
			Machine.emit(Prim.fieldref);
		} else {
			if(indirect) {
				Machine.emit(Op.LOADA, size, register, addr);
			} else {
				Machine.emit(Op.LOAD, size, register, addr);
			}
		}
	}
	
	public void call() {
		if(parent != null) {
			parent.load();
		} else {
			Machine.emit(Op.LOADA, size, Reg.OB, 0);
		}
	}
}
