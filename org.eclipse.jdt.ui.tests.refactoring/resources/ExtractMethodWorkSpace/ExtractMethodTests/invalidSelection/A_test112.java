package invalidSelection;

public class A_test112 {
	public void foo() {
		try {
			foo();
		} /*]*/catch (Exception e) {
			foo();
		}/*[*/
	}
}