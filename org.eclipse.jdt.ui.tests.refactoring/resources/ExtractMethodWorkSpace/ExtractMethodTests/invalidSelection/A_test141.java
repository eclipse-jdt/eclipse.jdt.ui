package invalidSelection;

public class A_test141 {
	private boolean b;
	public void foo() {
		/*]*/if (b)
			return;
		foo();
		return;/*[*/
	}
}