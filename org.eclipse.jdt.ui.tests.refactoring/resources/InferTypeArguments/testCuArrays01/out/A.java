package p;

import java.util.Arrays;
import java.util.List;

class A {
	List<String> l= null; 
	
	void add(String s) {
		l.add(s);
	}
	
	void addAll(String[] ss) {
		l.addAll(Arrays.asList(ss));
	}
	
	String[] get() {
		return l.toArray(new String[l.size()]);
	}
}
