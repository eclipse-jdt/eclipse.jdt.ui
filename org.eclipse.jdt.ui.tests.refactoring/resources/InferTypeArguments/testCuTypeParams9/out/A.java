package p;

import java.util.Vector;

class TypeParams9<T1 extends Number & Comparable> {
	Comparable f(T1 t1) {
		Vector<Comparable> v1 = new Vector<>();
		v1.add(t1);
		v1.add(Integer.valueOf(1));
		v1.add("");
		return v1.get(0);
	}
}
