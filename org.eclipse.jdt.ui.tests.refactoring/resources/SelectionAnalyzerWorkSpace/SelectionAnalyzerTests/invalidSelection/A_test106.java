package invalidSelection;

public class A_test106 {
	public void foo() {
		try {
			foo();
		} finally /*]*/{
			foo();
		}/*[*/
	}
}