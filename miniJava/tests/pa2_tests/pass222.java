// PA2 pass AST references in arguments and method invocations
class C {
    public void foo() {
        // "this" as reference and as qualifier
        this.foo(3, this);
        other.foo(4, other);
    }
}

