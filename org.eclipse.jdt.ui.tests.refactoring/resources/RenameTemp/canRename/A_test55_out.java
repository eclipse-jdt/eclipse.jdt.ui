package p;
class A<S extends Number & Cloneable> {
    S s;
    {
        S /*[*/t/*]*/;
        t= A.this.s;
    }
}