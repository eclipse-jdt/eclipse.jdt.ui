package p;

import java.util.ArrayList;
import java.util.List;

class A {
	void foo() {
		List<Super> list= new ArrayList<Super>();
		list.add(new Super());
		List<Super> l= list;
		l.add(new Sub());
	}
}

class Super {}
class Sub extends Super {}