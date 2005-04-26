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
        final A<String> name= new A<String>();
		A<String> s= name;
        A<T> a= new A<T>();
    }
}