package return_out;

public class A_test708 {
	boolean flag;
	public boolean foo() {
		return extracted();
	}
	protected boolean extracted() {
		/*[*/do {
			if (flag)
				foo();
			return true;
		} while(flag);/*]*/
	}
}
