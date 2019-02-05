package p;
class A implements I {
    public void m(I foo) {
        foo.m(foo);
    }
}