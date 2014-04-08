/*** no dereference from non-class types 
 * COMP 520
 * Identification
 */
class fail68 { 	
    public static void main(String[] args) {
        int c = 4;
        c = c.foo;
    }

    int foo;
}

