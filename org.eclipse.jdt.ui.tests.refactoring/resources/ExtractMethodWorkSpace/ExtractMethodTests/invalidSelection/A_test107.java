package invalidSelection;

public class A_test107 {
	public void foo() {
		try {
			foo();
		} finally {/*[*/
			foo();
		}/*[*/
	}
}