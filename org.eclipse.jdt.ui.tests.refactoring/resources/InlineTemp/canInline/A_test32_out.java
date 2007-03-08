package p;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class A {
	public A(List<? extends Number> arg) {
		method(Arrays.asList(1, 2, 3 + 4*5));
		method2(Arrays.asList(1, 2, 3 + 4*5));
		method3(Arrays.asList(1, 2, 3 + 4*5));
		new A(Arrays.asList(1, 2, 3 + 4*5));
		
		List<Integer> list2= Arrays.asList(1, 2, 3 + 4*5);
		list2= Arrays.asList(1, 2, 3 + 4*5);
		Object o= Arrays.asList(1, 2, 3 + 4*5);
	}

	void method(List<Integer> l) { }
	void method2(Collection<Integer> c) {	}
	void method3(Object o) {	}
}
