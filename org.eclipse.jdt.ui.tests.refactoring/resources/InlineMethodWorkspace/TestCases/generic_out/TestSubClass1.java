package generic_out;

class SuperClass1<T, E> {
	public void foo() {
		T t= null;
		E e= null;
	}
}

public class TestSubClass1<A, B> extends SuperClass1<A, B> {
	void bar() {
		A t= null;
		B e= null;
	}
}

