package generic_in;

public class TestClassInstance {
	void bar() {
		X<String, Number> x= new X<String, Number>();
		x./*]*/foo()/*[*/;
	}
}

class X<T, E> {
	public void foo() {
		T t= null;
		E e= null;
	}
}