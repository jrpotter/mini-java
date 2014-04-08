/**
 * COMP 520
 * Recursion
 */
class MainClass {
   public static void main (String [] args) {

      FirstClass f = new FirstClass ();
      f.s = new SecondClass ();
      f.s.f = f;

      f.testme ();
   }
}

class FirstClass
{
   int n;
   SecondClass s;

   public void testme ()
   {
      int tstvar = 10;

      System.out.println(8 + s.fact (3));
   }
}

class SecondClass
{
   int n;
   FirstClass f;

   public int fact (int param){
      int r = 1;
      if (param > 1)
          r = param * fact(param - 1);
      return r;
   }
}

