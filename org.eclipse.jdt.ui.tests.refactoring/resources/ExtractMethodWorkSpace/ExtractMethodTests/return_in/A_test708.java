package return_in;

public class A_test708 {
	boolean flag;
	public boolean foo() {
		/*[*/do {
			if (flag)
				foo();
			return true;
		} while(flag);/*]*/
	}
}
