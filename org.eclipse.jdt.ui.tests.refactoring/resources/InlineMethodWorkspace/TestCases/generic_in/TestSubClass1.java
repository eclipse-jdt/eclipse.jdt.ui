package generic_in;

class SuperClass1<T, E> {
	public void foo() {
		T t= null;
		E e= null;
	}
}

public class TestSubClass1<A, B> extends SuperClass1<A, B> {
	void bar() {
		/*]*/foo()/*[*/;
	}
}

