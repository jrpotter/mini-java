// PA1 parse decl pass
class Test {

    int [] a;
    Test [] t;

    void p() {
        void x = this.t[3].a[4].p();
    }
}

