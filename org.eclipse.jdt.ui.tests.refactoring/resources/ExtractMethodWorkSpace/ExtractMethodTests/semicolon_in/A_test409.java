package semicolon_in;

public class A_test409 {
	public void foo() {
		/*[*/synchronized (this) {
			foo();
		}/*]*/
	}
}