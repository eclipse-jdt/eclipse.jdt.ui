package validSelection;

public class A_test350 {

	public void foo() {
		/*]*/synchronized (this) {
			foo();
		}/*[*/
	}
}
