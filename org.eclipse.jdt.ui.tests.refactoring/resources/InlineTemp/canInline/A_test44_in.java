package p;

import java.util.Vector;

class A {
	void x() {
        Vector<? extends Integer> inline= new Vector<Integer>();
        Vector<? extends Number> var= inline;
	}
}
