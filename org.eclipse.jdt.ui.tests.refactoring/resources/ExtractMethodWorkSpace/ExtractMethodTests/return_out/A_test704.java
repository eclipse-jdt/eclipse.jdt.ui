package return_out;

public class A_test704 {
	private boolean flag;
	public boolean foo() {
		return extracted();
	}
	protected boolean extracted() {
		/*[*/do {
			return true;
		} while (flag);/*]*/
	}
}
