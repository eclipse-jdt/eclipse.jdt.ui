package p;

import java.util.ArrayList;
import java.util.List;

class A {
	void foo() {
		List l= new ArrayList();
		l.add(new Integer(1));
		Number n= (Number) l.get(0);
		Object o1= (Number) l.get(0);
		Object o2= l.get(0);
		
		List l2= new ArrayList();
		l2.add(n);
		Integer i= (Integer) l2.get(0);
	}
}