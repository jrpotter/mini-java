/*** can't redeclare local var in nested block
 * COMP 520
 * Identification
 */
class fail11 { 	
    public static void main(String[] args) {}
    
    public void foo(int parm) {
        int x = 0;
        {
            int x = 4;
        }
    }
}
