package p1;

public class A {

    B b;
    
    static class InnerInterface {
        void innerMethod() {
            
        }
    }
}

class B {

	public void m() {
	    A.InnerInterface inner = new A.InnerInterface();
	    inner.innerMethod();
	}
    
}
