package p;

import java.util.ArrayList;
import java.util.List;

public class A {
	void foo() {
		List<Object> l= new ArrayList<>();
		l.add("Eclipse");
		l.add(Integer.valueOf(10));
		bar(l);
	}
	void bar(List<Object> l) {
		l.add(new A());
	}
}