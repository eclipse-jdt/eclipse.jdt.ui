package validSelection;

public class A_test300 {
	public void foo() {
		try {/*[*/
			foo();
		/*]*/} catch (Exception e) {
			foo();
		}
	}
}