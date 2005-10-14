package p;

class A {
    void m() { }
}
class Sub extends A {
    @Override
    void m() { }
}
class Sub2 extends A {
    @Override @Deprecated void m() { }
}
