/*** can't assign to method name "f" in "b.f" on line 8
 * COMP 520
 * Identification
 */
class fail34 { 	
    public static void main(String[] args) {
        B b = new B();
	b.f = 5;
    }
}

class B {
	
	public int f() {return 3;}
}
