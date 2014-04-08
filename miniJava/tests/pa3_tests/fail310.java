/*** can't redeclare a parameter as a local var
 * COMP 520
 * Identification
 */
class fail10 { 	
    public static void main(String[] args) {}
    
    public void foo(int parm) {
        int x = 0;
        {
            int parm = 4;
        }
    }
}

