package p;

import java.util.Vector;

class A {

	void k() {
		Vector v1= new Vector();
		Vector v2= new Vector();
		Vector v3= new Vector();

		v3.add(new String("fff")); // String <= E[v3] --> String is
												// not parametric --> nothing to unify
		v2.add(v3);           // v3 <= E[v2] --> 2. unify (E[v3], E[E[v2]])
		v1.add(v2.get(0)); // E[v2] <= E[v1] --> 1. unify (E[E[v2]], E[E[v1]])
	}
}
