package generics_out;

public class A_test1101 {
	public <E> void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/foo();/*]*/
	}
}
