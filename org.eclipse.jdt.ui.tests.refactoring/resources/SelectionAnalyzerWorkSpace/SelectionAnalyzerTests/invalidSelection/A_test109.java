package invalidSelection;

public class A_test109 {
	public void foo() {
		/*]*/try {
			foo();
		} finally {
			foo();
		/*]*/}
	}
}