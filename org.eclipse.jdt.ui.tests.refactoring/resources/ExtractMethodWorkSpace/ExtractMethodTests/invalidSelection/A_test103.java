package invalidSelection;

public class A_test103 {
	public void foo() {
		try {
			foo();
		} catch (Exception e) /*]*/{
			foo();
		}/*[*/
	}
}