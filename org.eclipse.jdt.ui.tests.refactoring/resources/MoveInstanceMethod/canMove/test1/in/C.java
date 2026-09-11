package p3;

import p1.A;
import p2.B;

class C {
    C() {
    	A a = getA();
    	B b = getB();
		a.mA1(b);
	}

	A getA() {
		return null;
	}

	B getB() {
		return null;
	}
}