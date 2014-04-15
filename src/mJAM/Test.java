/**
 * Example illustrating components of mJAM package
 * @author prins
 * @version COMP 520 V2.2
 */
package mJAM;
import mJAM.Machine.Op;
import mJAM.Machine.Reg;
import mJAM.Machine.Prim;

// test class to construct and run an mJAM program
public class Test
{
	public static void main(String[] args){
		
		Machine.initCodeGen();
		System.out.println("Generating test program object code");
		
	/* class A {
	 *    int x;  
	 *    int p(){return x;} 
	 * }  
	 */
		Machine.emit(Op.LOADL,11);           // hello
		Machine.emit(Prim.putintnl);
		int patchme_coA = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP,Reg.CB,0);      // jump around methods of class A (branch to /*coA*/)
		
		// code for p() in A
		int label_pA = Machine.nextInstrAddr();
/*pA*/	Machine.emit(Op.LOAD,Reg.OB,0);      // x at offset 0 in current instance of A
		Machine.emit(Op.HALT,4,0,0);
		Machine.emit(Op.RETURN,1,0,0);       // return one value, pop zero args
		
		// build class object for A at 0[SB]
		int label_coA = Machine.nextInstrAddr();
		Machine.patch(patchme_coA, label_coA);
/*coA*/ Machine.emit(Op.LOADL,-1);              // no superclass object     
		Machine.emit(Op.LOADL,1);               // number of methods
		Machine.emit(Op.LOADA,Reg.CB,label_pA); // code addr of p_A
		
		
    /* class B extends A {
     *	  int y;  
     *    int p(){return x + 22;} 
   	 * }
   	 */
		int patchme_coB = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP,Reg.CB,0);     // branch around methods in class B
		
		// code for p() in B
		int label_pB = Machine.nextInstrAddr();
/*pB*/  Machine.emit(Op.LOAD,Reg.OB,0);    // x at offset 0 in current instance
		Machine.emit(Op.LOADL,22);
		Machine.emit(Op.HALT,4,0,0);
		Machine.emit(Prim.add);
		Machine.emit(Op.RETURN,1,0,0);    // return one value, pop zero args
		
		// build class object for B at 3[SB]
		int label_coB = Machine.nextInstrAddr();
		Machine.patch(patchme_coB, label_coB);
/*coB*/ Machine.emit(Op.LOADA,Reg.SB,0);        // addr of superclass object
		Machine.emit(Op.LOADL,1);               // number of methods
		Machine.emit(Op.LOADA,Reg.CB,label_pB); // code addr of p_B

		
	/* class C { 
	 *    public static void main(String [] args) {
	 *      A a = new  A(); 
	 *      a.x = 33; 
	 *      System.out.println(a.p()); 
	 *      ...
	 */
		int patchme_coC = Machine.nextInstrAddr();
		Machine.emit(Op.JUMP,Reg.CB,0);    // branch around methods of class C
		
		// code for main() in C
		int label_mainC = Machine.nextInstrAddr();
/*mainC*/  Machine.emit(Op.HALT,4,0,0);
                                          // local var "a" will be at 3[LB] after init
		Machine.emit(Op.LOADA,Reg.SB,0);  // class descriptor for A
		Machine.emit(Op.LOADL,1);         // size of A
		Machine.emit(Prim.newobj);        // result addr becomes value of "a"
		Machine.emit(Op.LOAD,Reg.LB,3);   // value of "a" (heap addr)
		Machine.emit(Op.LOADL,0);         // "x" is field 0 in A
		Machine.emit(Op.LOADL,33);        // new value 33
		Machine.emit(Op.HALT,4,0,0);
		Machine.emit(Prim.fieldupd);      // a.x = 33
		Machine.emit(Op.LOAD,Reg.LB,3);   // addr of instance "a" on heap  
		Machine.emit(Op.CALLI,Reg.CB,label_pA);  //  call to known instance method p_A
		Machine.emit(Prim.putintnl);       // print result
		
	/*      ...
	 *      A b = new  B(); 
	 *      b.x = 44;  
	 *      System.out.println(b.p()); 
	 *   } // end main
	 * } // end class C
	 */
										  // local var "b" will be at 4[LB] after init
		Machine.emit(Op.LOADA,Reg.SB,3);  // class descriptor for B
		Machine.emit(Op.LOADL,2);         // size of B
		Machine.emit(Prim.newobj);        // result addr becomes value of "b"
		Machine.emit(Op.LOAD,Reg.LB,4);   // fetch b
		Machine.emit(Op.LOADL,0);         // field 0
		Machine.emit(Op.LOADL,44);        // b.x = 44
		Machine.emit(Prim.fieldupd); 
		Machine.emit(Op.HALT,4,0,0);
		Machine.emit(Op.LOAD,Reg.LB,4);   // addr of instance "b" 
		Machine.emit(Op.CALLD,0);         // dynamic call, method index 0 (= method p)
		Machine.emit(Prim.putintnl);      // print result
		Machine.emit(Op.RETURN,0,0,1);    // return no value (void), pop 1 arg (= String [] args)
		
		// build class descriptor for C at 6[SB]
		int label_coC = Machine.nextInstrAddr();
		Machine.patch(patchme_coC, label_coC);
/*coC*/ Machine.emit(Op.LOADL,-1);              // no superclass object     
		Machine.emit(Op.LOADL,0);               // number of methods = 0
		
	/*
	 *  End of class declarations - call main
	 */
		Machine.emit(Op.LOADL,Machine.nullRep);    // put null on stack as value of main's arg
		Machine.emit(Op.CALL,Reg.CB,label_mainC);  // call known static main()
		Machine.emit(Op.LOADL,88);                 // goodbye
		Machine.emit(Prim.putintnl);		
		Machine.emit(Machine.Op.HALT,0,0,0);       // halt

		/* write code as an object file */
		String objectCodeFileName = "test.mJAM";
		ObjectFile objF = new ObjectFile(objectCodeFileName);
		System.out.print("Writing object code file " + objectCodeFileName + " ... ");
		if (objF.write()) {
			System.out.println("FAILED!");
			return;
		}
		else
			System.out.println("SUCCEEDED");	
		
		/* create asm file using disassembler */
		String asmCodeFileName = "test.asm";
		System.out.print("Writing assembly file ... ");
		Disassembler d = new Disassembler(objectCodeFileName);
		if (d.disassemble()) {
			System.out.println("FAILED!");
			return;
		}
		else
			System.out.println("SUCCEEDED");
		
		/* run code */
		System.out.println("Running code ... ");
		Interpreter.debug(objectCodeFileName, asmCodeFileName);
		// Interpreter.interpret(objectCodeFileName);

		System.out.println("*** mJAM execution completed");
	}
}
