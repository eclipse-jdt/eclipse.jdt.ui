package p;

import java.util.ArrayList;
import java.util.List;

class A {
	void foo() {
		ArrayList/*<String>*/<String> l= new ArrayList<String>();
		l.add(new String());
	}
}