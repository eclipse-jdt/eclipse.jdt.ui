package p1;

public class A {

    B b;
    int k;
    
    private A() {

    }
    
    public void m() {
        A a = new A();
        a.k = 3;
    }
}
