package p3;

import p1.A;
import p2.B;

class C {
	{
		this.getA().m1A(getB());
	}
	
	A getA() {
		return null;
	}
	
	B getB() {
		return null;	
	}
}