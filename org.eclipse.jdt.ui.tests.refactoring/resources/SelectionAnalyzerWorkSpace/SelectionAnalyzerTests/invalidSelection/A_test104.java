package invalidSelection;

public class A_test104 {
	public void foo() {
		try {
			foo();
		} catch (Exception e) {/*[*/
			foo();
		}/*[*/
	}
}