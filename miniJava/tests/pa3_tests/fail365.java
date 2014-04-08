/*** no access to a private method from outside class via qualified ref
 * COMP 520
 * Identification
 */
class fail65 { 	
    public static void main(String[] args) {
	F05 c = new F05();
	c.next.next.foo();
    }
}

class F05 {
    public F05 next;
    private void foo() {}
}
