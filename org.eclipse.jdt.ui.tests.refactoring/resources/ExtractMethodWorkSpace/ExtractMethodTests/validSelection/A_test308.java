package validSelection;

public class A_test308 {
	public void foo() {
		try {
			foo();
		} finally {/*[*/
			foo();
		/*]*/}
	}
}