package generics_out;

public class A_test1102 {
	public <E> void foo() {
		E local;
		extracted();
	}

	protected void extracted() {
		/*[*/foo();/*]*/
	}
}
