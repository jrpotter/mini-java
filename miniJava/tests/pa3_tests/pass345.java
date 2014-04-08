/**
 * COMP 520
 * Object creation and field update
 */
class MainClass {
   public static void main (String [] args) {

      int tstvar = 0;

      FirstClass f = new FirstClass ();
      tstvar = 5 + f.n;

      System.out.println(tstvar);
   }
}

class FirstClass
{
   int n;

}



