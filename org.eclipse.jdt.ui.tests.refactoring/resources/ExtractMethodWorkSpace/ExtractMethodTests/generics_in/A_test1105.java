package generics_in;

public class A_test1105 {
	public <E> void foo(E param) {
		/*[*/E local= param;
		foo(local);/*]*/
	}
}
