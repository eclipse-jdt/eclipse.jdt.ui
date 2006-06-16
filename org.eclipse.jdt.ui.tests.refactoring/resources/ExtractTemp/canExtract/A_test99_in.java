package p; //7, 32, 7, 36

import java.util.*;

class A<E> extends ArrayList<E> {
	void inspect() {
		for (Iterator<E> iter= this.iterator(); iter.hasNext();) {
			iter.next();
		}
	}
}