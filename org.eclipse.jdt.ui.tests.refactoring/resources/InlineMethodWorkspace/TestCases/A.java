public class A {
	public void foo() {
		bar();
	}
	int bar() {
		return baz();
	}
	
	int baz() {
		return 10;
	}
}
