package p;
class A implements I {
    public I m(I foo) {
        foo.m(foo);
        return null;
    }
}