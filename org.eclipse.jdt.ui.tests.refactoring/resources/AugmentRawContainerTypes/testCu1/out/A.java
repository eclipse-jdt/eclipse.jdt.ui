package p;

import java.util.ArrayList;
import java.util.List;

class A {
	void foo() {
		List<String>/*<String>*/ l= new ArrayList();
		l.add(new String());
	}
}