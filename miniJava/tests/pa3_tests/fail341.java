/*** incorrect type result returned from function
 * COMP 520
 * Type checking
 */
class T41 {
    public static void main(String [] args) {
        T41 h = new T41();
        int x = h.foo();
    }

    // return type not compatible with declared return type
    public int foo() {
        return 3 < 4;
    }
}

