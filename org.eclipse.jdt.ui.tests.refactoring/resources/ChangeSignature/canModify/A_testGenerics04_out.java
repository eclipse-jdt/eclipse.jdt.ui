package p;

import java.util.List;

class A<E> {
	public <T extends Number> void m(A<String> as, List<Integer> li) {}
}

class Sub<E> extends A<E> {
	public <T extends Number> void m(A<String> as, List<Integer> li) {}
	
	void test() {
		A<String> as= new A<String>();
		as.m(as, null);
		new Sub<Double>().m(as, null);
	}
}