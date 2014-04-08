/*** no access to private method from outside the class
 * COMP 520
 * Identification
 */
class fail64 { 	
    public static void main(String[] args) {
	F04 c = new F04();
	c.foo();
    }
}

class F04 {
    public F04 next;
    private void foo() {}
}
