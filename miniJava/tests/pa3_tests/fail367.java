/*** no dereference from non-class types 
 * COMP 520
 * Identification
 */
class F67 { 	
    public static void main(String[] args) {
        F05 c = new F05();
        c = c.foo.next;
    }
}

class F05 {
    public F05 next;
    public int foo;
}
