package p;

import java.util.ArrayList;
import java.util.List;

class A<E> {
	public <T extends Number> void m(List<Integer> li, A<String> as) {}
}

class Sub<E> extends A<E> {
	public <T extends Number> void m(List<Integer> li, A<String> as) {}
	
	void test() {
		A<String> as= new A<String>();
		as.m(new ArrayList<Integer>(1), as);
		new Sub<Double>().m(new ArrayList<Integer>(2), as);
	}
}