package p;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class A {
	public A(List<? extends Number> arg) {
		List<Integer> list= Arrays.asList(1, 2, 3 + 4*5);
		method(list);
		method2(list);
		method3(list);
		new A(list);
		
		List<Integer> list2= list;
		list2= list;
		Object o= list;
	}

	void method(List<Integer> l) { }
	void method2(Collection<Integer> c) {	}
	void method3(Object o) {	}
}
