package generic_in;

class SuperClass2<T, E> {
	public void foo() {
		T t= null;
		E e= null;
	}
}

public class TestSubClass2<A, B> extends SuperClass2<B, A> {
	void bar() {
		/*]*/foo()/*[*/;
	}
}

