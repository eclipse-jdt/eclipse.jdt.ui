package generics_out;

public class A_test1100<E> {
	public void foo() {
		extracted();
	}

	protected void extracted() {
		/*[*/foo();/*]*/
	}
}
