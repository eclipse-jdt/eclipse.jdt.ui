package p;
class A<S extends Number & Cloneable> {
    S s;
    {
        S /*[*/s/*]*/;
        s= A.this.s;
    }
}