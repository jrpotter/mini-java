/*** can't access non-static field y from within a static context in ref C.y
 * COMP 520
 * Identification
 */
class C { 	
    public static void main(String[] args) { }

    public void f() {
        int x = C.y;
    }

    public int y;
}
