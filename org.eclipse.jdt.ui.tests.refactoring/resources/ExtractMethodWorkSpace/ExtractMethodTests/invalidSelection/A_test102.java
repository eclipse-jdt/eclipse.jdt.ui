package invalidSelection;

public class A_test102 {
	public void foo() {
		try {/*[*/
			foo();
		}/*[*/ catch (Exception e) {
			foo();
		}
	}
}