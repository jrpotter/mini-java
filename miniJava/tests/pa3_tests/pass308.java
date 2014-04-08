/**
 * COMP 520
 * Identification
 */
class Pass08 { 	
    public static void main(String[] args) {
        A08 a08 = new A08();
        Pass08 p08 = new Pass08();
        p08.a = a08;
        a08.p = p08;
        int y = p08.a.p.a.x;
    } 
    
    public A08 a;
}

class A08 { 	
    public Pass08 p;
    public int x;
}
