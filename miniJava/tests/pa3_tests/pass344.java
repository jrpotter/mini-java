/**
 * COMP 520
 * repetitive statement
 */
class MainClass {
   public static void main (String [] args) {
      int tstvar = 0;

      tstvar = 3;
      int i = 0;
      while (i < 4) {
          i = i + 1;
          tstvar = i;
      }

      System.out.println(tstvar);
   }
}


