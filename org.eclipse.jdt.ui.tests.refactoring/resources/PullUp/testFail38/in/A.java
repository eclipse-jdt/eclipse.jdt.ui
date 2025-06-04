package p;

class A {
}
class C extends A {
    public void m() {
    }
}
class B extends A {
    // pull up m()
    static void m() {
    }
}