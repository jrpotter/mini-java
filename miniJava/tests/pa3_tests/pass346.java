/**
 * COMP 520
 * Object creation and update
 */
class MainClass {
   public static void main (String [] args) {

      int tstvar = 0;

      FirstClass f = new FirstClass ();
      f.s = new SecondClass ();

      // write and then read;
      f.s.n = 6;
      tstvar = f.s.n;

      System.out.println(tstvar);
   }
}

class FirstClass
{
   int n;
   SecondClass s;

}

class SecondClass
{
   int n;
   FirstClass f;

}



