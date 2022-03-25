//can rename A.m(String[]) to k
package p;

class A<E> {
    void m(E[] e) {}
    void m(String[] t) {};
}
class Sub extends A<String> {
    void m(String... s) {}
}