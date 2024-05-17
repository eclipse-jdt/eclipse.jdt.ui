package p1;

import p1.A.InnerInterface;

class B {

	public void m(A a) {
	    InnerInterface inner = a.new InnerInterface();
	    inner.innerMethod();
	}
    
}
