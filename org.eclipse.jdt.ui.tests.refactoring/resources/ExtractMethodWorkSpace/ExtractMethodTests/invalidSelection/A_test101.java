package invalidSelection;

public class A_test101 {
	public void foo() {
		try /*]*/{
			foo();
		/*]*/} catch (Exception e) {
			foo();
		}
	}
}