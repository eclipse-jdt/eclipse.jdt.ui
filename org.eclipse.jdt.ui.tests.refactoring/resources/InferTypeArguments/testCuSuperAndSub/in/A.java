package p;

import java.util.ArrayList;
import java.util.List;

class A {
	void foo() {
		List list= new ArrayList();
		list.add(new Super());
		List l= list;
		l.add(new Sub());
	}
}

class Super {}
class Sub extends Super {}