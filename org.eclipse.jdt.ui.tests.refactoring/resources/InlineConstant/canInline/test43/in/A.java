package p;

class A {
    private static final CharSequence n = null;
    void f(CharSequence s) {
        System.out.println("CharSequence");
    }
    void f(String s) {
        System.out.println("String");
    }
    void g() {
        f(n);
    }
}