/**
 * COMP 520
 */
class MainClass {
   public static void main (String [] args) {

      FirstClass f = new FirstClass ();
      f.n = 56;

      f.set (999);
      System.out.println(f.get());
   }
}

class FirstClass
{
   int n;

   public void set (int value)
   {
      int temp = 0;

      temp = value;
      
      n = temp;
   }

   public int get ()
   {
      return n;
   }
}
