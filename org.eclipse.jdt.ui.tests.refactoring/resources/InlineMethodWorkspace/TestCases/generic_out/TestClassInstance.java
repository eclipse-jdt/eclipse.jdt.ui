package generic_out;

public class TestClassInstance {
	void bar() {
		X<String, Number> x= new X<String, Number>();
		String t= null;
		Number e= null;
	}
}

class X<T, E> {
	public void foo() {
		T t= null;
		E e= null;
	}
}