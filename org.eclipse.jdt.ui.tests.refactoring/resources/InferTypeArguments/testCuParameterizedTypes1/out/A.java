package p;

class A<T> {
    class E implements I<T> {
        public void foo() {
            E m = (E) this;
        }
    }
}

interface I<K> {
}
