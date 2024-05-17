package p1;

public class A {

    B b;
    
    protected class InnerInterface {
        void innerMethod() {
            
        }
    }
    
    public void m() {
        InnerInterface inner = new InnerInterface();
        inner.innerMethod();
    }
}

class B {
    
}
