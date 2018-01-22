package p;

class A {
    void call(My my) {
        my.method("Eclipse1", Integer.valueOf(1));
    }
}

class My<C> {
    <M> void method(C c, M m) {}
}
