//can rename A.m(String...) to k
package p;

class Sup<E> {
    void m(E[] e) {}
    void m(String[] t) {};
}
class A extends Sup<String> {
    void m(String... s) {}
}