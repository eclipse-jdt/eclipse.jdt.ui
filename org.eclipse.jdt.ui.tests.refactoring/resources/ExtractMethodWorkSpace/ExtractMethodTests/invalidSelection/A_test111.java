package invalidSelection;

public class A_test111 {
	public void foo() {
		try {
			foo();
		} catch (/*]*/Exception e/*[*/) {
			foo();
		}
	}
}