// PA2 pass AST multiple classes
class A {}

class B {
    private int [] v;  

    public void foo(int a, B other) {}

    C c;

    private int x;
}

class C {
    private boolean b;

    public int[] tryit() {
        int x =-/* unary */-x;
        return (new int [20]);  
    }
}
