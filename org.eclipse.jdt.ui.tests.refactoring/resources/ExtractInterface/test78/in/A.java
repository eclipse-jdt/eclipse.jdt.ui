package p;
class A {
    public A m(A foo) {
        foo.m(foo);
        return null;
    }
}