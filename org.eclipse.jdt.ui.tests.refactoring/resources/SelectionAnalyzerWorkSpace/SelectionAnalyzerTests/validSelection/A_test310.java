package validSelection;

public class A_test310 {
	public void foo() {
		try {
			foo();
		} catch (Exception e) {
			foo();
		}
		/*]*/foo();/*[*/
	}
}