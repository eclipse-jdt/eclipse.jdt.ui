package invalidSelection;

public class A_test100 {
	public void foo() {
		try /*]*/{
			foo();
		}/*[*/ catch (Exception e) {
			foo();
		}
	}
}