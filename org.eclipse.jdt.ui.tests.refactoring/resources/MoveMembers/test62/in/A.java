package p;

public class A {
	
	public static class SomeInner<T> {
	}
	
	public void foo() {
		new SomeInner<String>();
	}
	
}
