package invalidSelection;

public class A_test114 {
	public void foo() {
		try {
			foo();
		} /*]*/finally {
			foo();
		}/*[*/
	}
}