/**
 * COMP 520
 * Identification
 */
class Pass07 { 	
    public static void main(String[] args) {
        Pass07 p07 = new Pass07();
        p07.next = p07;
        p07.next.next.x = 3;
    } 
    
    public Pass07 next;
    private int x;
}
