package p;
public class A<T> {
    enum TEST {
        FIRST, SECOND
    }
    void foo() {
        B<T> b= getB();
    }
    B<T> getB() {
        A.TEST test= TEST.FIRST;
        return null;
    }
    void bar() {
        A<String> s= new A<String>();
        final A<T> a2= new A<T>();
		A<T> a= a2;
    }
}