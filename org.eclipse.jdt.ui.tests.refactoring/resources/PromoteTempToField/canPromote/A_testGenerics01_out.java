//9, 9, 9, 11
package p;

import java.util.Vector;

class A<T> {
	private Vector<T> fVt= new Vector<T>();

	void m() {
		fVt.clear();
		fVt= new Vector();
	}
}