/*** duplicate declaration of class "System" in line 11 (cannot hide predefined classes)
 * COMP 520
 * Identification
 */
class Fail09 {
    public static void main(String[] args) {
	System.out.println(5);
    }
}

class System {
    public int x;
}