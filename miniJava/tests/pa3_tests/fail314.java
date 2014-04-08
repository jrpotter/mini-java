/*** var decl can not reference id in initializing expression
 * COMP 520
 * Identification
 */
class fail14 { 	
    public static void main(String[] args) {}

    int x;
    int y;

    public void foo() {
	int x = y + x;
    }
}

