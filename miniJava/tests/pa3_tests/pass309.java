/**
 * COMP 520
 * Identification
 */
class Pass09 { 	
    public static void main(String[] args) {
        F03 c = new F03();
        c.next.mynext.x = 3;
    }

    private F03 mynext;  // normally no access, but ok if dereferenced within Pass09
}

class F03 {
    public Pass09 next;
    public int x;
}
