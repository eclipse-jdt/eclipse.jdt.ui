package p;

import java.util.Vector;

class TypeParams9<T1 extends Number & Comparable> {
	Comparable f(T1 t1) {
		Vector v1 = new Vector();
		v1.add(t1);
		v1.add(new Integer(1));
		v1.add("");
		return (Comparable) v1.get(0);
	}
}
