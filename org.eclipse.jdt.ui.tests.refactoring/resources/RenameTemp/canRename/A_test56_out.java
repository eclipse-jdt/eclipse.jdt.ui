package p;
class A<S extends Number & Cloneable> {
    void m(S /*[*/param/*]*/) {
        param.byteValue();
    }
}