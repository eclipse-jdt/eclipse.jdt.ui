package p;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

class A {
	void foo() {
		AbstractList l= new ArrayList();
		List list= l;
		list.add("Eclipse");
	}
}