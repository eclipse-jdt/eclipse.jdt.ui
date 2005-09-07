package p;

import java.util.Vector;

class A {
	void foo() {
		Vector<Vector<Vector>> v1= new Vector<Vector<Vector>>();
		Vector<Vector> v2= new Vector<Vector>();
		v2.add(v1);
		v1.add(v2);
	}
	void bar() {
		Vector<Vector> v3= new Vector<Vector>();
		v3.add(v3);
	}
}