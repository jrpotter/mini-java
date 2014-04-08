/*** can't access non-static method from within a static method
 * COMP 520
 * Identification
 */
class fail53 { 	
    public static void main(String[] args) {
        int y = f() + 3;
    }

    public int f() {
        return 7;
    }
}