package p;
class A<S extends Number & Cloneable> {
    <S> void m(S /*[*/param/*]*/) {
        param.byteValue();
    }
}