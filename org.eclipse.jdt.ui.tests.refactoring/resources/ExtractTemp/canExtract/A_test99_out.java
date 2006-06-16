package p; //7, 32, 7, 36

import java.util.*;

class A<E> extends ArrayList<E> {
	void inspect() {
		A<E> temp= this;
		for (Iterator<E> iter= temp.iterator(); iter.hasNext();) {
			iter.next();
		}
	}
}