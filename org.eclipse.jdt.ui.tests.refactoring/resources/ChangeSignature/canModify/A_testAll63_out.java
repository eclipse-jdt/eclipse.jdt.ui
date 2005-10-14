package p;

class A {
    protected void m() { }
}
class Sub extends A {
    @Override
	protected
    void m() { }
}
class Sub2 extends A {
    @Override @Deprecated
	protected void m() { }
}
