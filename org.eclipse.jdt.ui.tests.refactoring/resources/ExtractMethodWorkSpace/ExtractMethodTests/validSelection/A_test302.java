package validSelection;

public class A_test302 {
	public void foo() {
		try {
			foo();
		} catch (Exception e) {/*[*/
			foo();
		/*]*/}
	}
}