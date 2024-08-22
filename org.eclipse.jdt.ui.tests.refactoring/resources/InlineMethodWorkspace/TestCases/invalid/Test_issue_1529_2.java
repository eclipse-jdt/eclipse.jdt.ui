package invalid;

class A {
    public int x;
}

class B extends A {
    void foo() {
        super.x = 8;
    }

    static void err(B b) {
        b./*]*/foo()/*[*/;
    }
}

