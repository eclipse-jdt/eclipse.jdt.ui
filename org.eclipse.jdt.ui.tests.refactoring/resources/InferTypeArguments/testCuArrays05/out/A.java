package p;

import java.util.List;

class A {
	List<String> l= null;
	String[] get() {
		return l.toArray(new String[l.size()]);
	}
}
