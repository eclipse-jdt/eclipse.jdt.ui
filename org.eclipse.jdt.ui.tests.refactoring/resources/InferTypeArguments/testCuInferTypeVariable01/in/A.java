package p;

import java.util.Vector;

class Cell {
	public <T> T f1(T l) {
		Vector v= new Vector();
		v.add(l);
		return (T) v.get(0);
	}
}