package p;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

class A {
	void foo() {
		AbstractList<String> l= new ArrayList<String>();
		List<String> list= l;
		list.add("Eclipse");
	}
}