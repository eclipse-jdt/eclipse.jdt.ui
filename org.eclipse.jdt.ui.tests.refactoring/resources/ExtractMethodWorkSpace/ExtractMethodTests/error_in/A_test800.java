package error_in;

public class A_test800 {
	public void fails() {
		foo()
	}
	public void foo() {
		/*[*/foo();/*]*/
	}
}
