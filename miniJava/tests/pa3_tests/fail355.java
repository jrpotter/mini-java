/*** non-static method f() can not be referenced from a static context in ref D.f()
 * COMP 520
 * Identification
 */
class D { 	
    public static void main(String[] args) { }

    public int f(){
        return D.f();
    }
}
