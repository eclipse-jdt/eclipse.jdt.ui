package p;
public class A<T> {
    enum TEST {
        FIRST, SECOND
    }
    void foo() {
        B<T> b= getB();
    }
    B<T> getB() {
        final A.TEST test2= TEST.FIRST;
        A.TEST test= test2;
        return null;
    }
    void bar() {
        A<String> s= new A<String>();
        A<T> a= new A<T>();
    }
}