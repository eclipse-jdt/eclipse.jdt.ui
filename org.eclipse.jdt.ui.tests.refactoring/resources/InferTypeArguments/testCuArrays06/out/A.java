package p;

import java.util.List;

class A {
	List<String> l= null;
	
	void add(String s) {
		l.add(s);
	}
	
	String[] get() {
		return l.toArray(new String[l.size()]);
	}
}
