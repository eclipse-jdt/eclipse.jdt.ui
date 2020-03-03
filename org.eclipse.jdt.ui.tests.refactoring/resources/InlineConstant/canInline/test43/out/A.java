package p;

class A {
    void f(CharSequence s) {
        System.out.println("CharSequence");
    }
    void f(String s) {
        System.out.println("String");
    }
    void g() {
        f((CharSequence) null);
    }
}