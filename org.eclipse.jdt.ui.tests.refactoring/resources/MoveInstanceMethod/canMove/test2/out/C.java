package p3;

import p1.A;
import p2.B;

class C {
	C() {
		A a= new A();
		new B().mA1(a);
	}	
}