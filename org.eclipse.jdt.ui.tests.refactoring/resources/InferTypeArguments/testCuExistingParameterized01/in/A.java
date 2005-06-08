package p;

import java.util.ArrayList;
import java.util.List;

class A {
	
	void foo() {
		List l= new ArrayList();
		bar(l);
		l.add(new Object());
	}
	
	void bar(List<String> l) {
		
	}
}