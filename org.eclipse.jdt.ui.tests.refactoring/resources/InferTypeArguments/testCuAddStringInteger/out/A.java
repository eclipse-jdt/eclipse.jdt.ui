package p;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class A {
	void foo() {
		List<Serializable> l= new ArrayList<Serializable>();
		l.add("Eclipse");
		l.add(new Integer(10));
		bar(l);
	}
	void bar(Object o) {
		((List)o).add(new A());
	}
}