package validSelection;

public class A_test307 {
	public void foo() {
		try {
			foo();
		} finally {/*[*/
			foo();
		/*]*/}
	}
}