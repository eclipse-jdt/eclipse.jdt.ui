package generics_in;

public class A_test1106<E> {
	public void foo(E param) {
		/*[*/E local= param;
		foo(local);/*]*/
	}
}
