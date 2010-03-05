package p;

import p.A.X.Inner;

class A {
	class X {
		class Inner {
			
		}
	}
}
class Secondary {
	void f(){
		Inner x= new A().new X().new Inner();
			
	}
}