package p;

import java.util.List;

class A {
	List l= null;
	
	void add(String s) {
		l.add(s);
	}
	
	String[] get() {
		return (String[]) l.toArray(new String[l.size()]);
	}
}
