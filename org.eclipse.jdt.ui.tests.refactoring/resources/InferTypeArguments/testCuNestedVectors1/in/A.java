package p;

import java.util.Vector;

class A {
	void foo() {
		Vector v1= new Vector();
		Vector v2= new Vector();
		v2.add(v1);
		v1.add(v2);
	}
	void bar() {
		Vector v3= new Vector();
		v3.add(v3);
	}
}