package invalidSelection;

public class A_test110 {
	public void foo() {
		try/*[*/{
			foo();
		} finally {
			foo();
		}/*[*/
	}
}