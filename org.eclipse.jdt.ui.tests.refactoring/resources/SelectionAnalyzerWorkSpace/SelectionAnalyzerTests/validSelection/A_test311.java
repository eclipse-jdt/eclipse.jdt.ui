package validSelection;

public class A_test311 {
	public void foo() {
		/*]*/foo();/*[*/
		try {
			foo();
		} catch (Exception e) {
			foo();
		}
	}
}