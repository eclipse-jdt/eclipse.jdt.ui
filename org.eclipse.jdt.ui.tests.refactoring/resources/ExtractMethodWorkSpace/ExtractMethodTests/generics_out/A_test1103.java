package generics_out;

public class A_test1103 {
	public <E> void foo() {
		extracted();
		E local;
	}

	protected void extracted() {
		/*[*/foo();/*]*/
	}
}
