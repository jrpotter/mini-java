/**
 * COMP 520
 * Qualified references
 */
class MainClass {
   public static void main (String [] args) {

      int tstvar = 0;

      tstvar = 6;
      FirstClass f = new FirstClass ();
      f.s = new SecondClass ();

      // write and then read;
      f.s.f = f;
      f.s.f.n = tstvar + 1;
      tstvar = f.n;

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



