//9, 9, 9, 11
package p;

import java.util.Vector;

class A<T> {
	void m() {
		Vector<T> vt= new Vector<T>();
		vt.clear();
		vt= new Vector();
	}
}