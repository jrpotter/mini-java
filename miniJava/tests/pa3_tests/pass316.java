/**
 * COMP 520
 * Type Checking
 */
class Pass16 { 	
	public static void main(String[] args) {
		Pass16 a = new Pass16();
		boolean c = a.b() && a.p() == 5;			
	}
	
	int p() {return 5;}
	
	boolean b() {return true == false;}
}

