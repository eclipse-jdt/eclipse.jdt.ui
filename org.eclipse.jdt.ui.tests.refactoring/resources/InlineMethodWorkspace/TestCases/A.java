public class A {
	void bar() {
		X<String> x= new X<String>();
		x.foo();
	}
}

class X<T> {
	public void foo() {
		T t= null;
		t.toString();
	}
}
