package validSelection;

public class A_test351 {

	public void foo() {
		foo();/*[*/
		synchronized (this) {
			foo();
		}
		/*]*/foo();
	}
}
