package p;

import java.util.List;

class A {
	List l= null;
	String[] get() {
		return (String[]) l.toArray(new String[l.size()]);
	}
}
