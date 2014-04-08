/*** can't access non-static fields from within a static method
 * COMP 520
 * Identification
 */
class fail52 { 	
    public static void main(String[] args) {
        int y = x + 3;
    }
    
    public int x;
}
