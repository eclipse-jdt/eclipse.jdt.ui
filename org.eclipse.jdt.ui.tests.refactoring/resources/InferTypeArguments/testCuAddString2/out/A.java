package p;

import java.util.ArrayList;
import java.util.AbstractList;

class A {
	void foo() {
		AbstractList<String> l= new ArrayList<>();
		l.add(new String());
		l.add("");
	}
}