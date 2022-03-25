//can rename A.m(String...) to k
package p;

class Sup<E> {
    void k(E[] e) {}
    void k(String[] t) {};
}
class A extends Sup<String> {
    void k(String... s) {}
}