package generic_in;

public class TestClassInstance2<T, E> {
	public void toInline() {
		T t= null;
		E e= null;
	}
}

class TestClassInstance2References {
	void bar() {
		TestClassInstance2<String, Number> x= new TestClassInstance2<String, Number>();
		x.toInline();
	}
	void baz() {
		TestClassInstance2<Integer, Boolean> x= new TestClassInstance2<Integer, Boolean>();
		x.toInline();
	}
}
