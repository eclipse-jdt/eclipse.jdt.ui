package p;

class A {
    void call(My my) {
        my.method("Eclipse1", new Integer(1));
    }
}

class My<C> {
    <M> void method(C c, M m) {}
}
