package miniJava.CodeGenerator;

import mJAM.Machine.Reg;

public class RuntimeEntity {

    public Reg reg;
    public int size;
    public int addr;

    RuntimeEntity parent = null;

    public RuntimeEntity(int size, int addr, Reg reg) {
        this.reg = reg;
        this.size = size;
        this.addr = addr;
    }

}
