/*** return value does not agree with declared return type of function 
 * COMP 520
 * Type Checking
 */
class fail32 { 	
    public static void main(String[] args) {
	fail32 f = new fail32();
	f.noresult();
    }
    
    public void noresult() {
	return 10;
    }
}
