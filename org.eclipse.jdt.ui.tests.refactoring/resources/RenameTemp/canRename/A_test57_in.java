package p;
class A<S extends Number & Cloneable> {
    <S> void m(S /*[*/arg/*]*/) {
        arg.byteValue();
    }
}