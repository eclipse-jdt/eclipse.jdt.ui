package invalidSelection;

public class A_test116 {
	public void foo() {
		try {
			foo();
		} catch (/*]*/Exception e/*[*/) {
		}
	}
}
