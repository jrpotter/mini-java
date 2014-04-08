/*** limit cascaded errors: should report single type error
 * COMP 520
 * Identification
 */
class Fail42 {
    public static void main(String[] args) {
        int x = 2 + (3 + (4 + (5 + (1 != 0))));
    }
}


