package invalidSelection;

public class A_test145 {
	boolean flag;
	public boolean foo() {
		/*]*/do {
			if (flag)
				break;
			return false;
		} while (flag);/*[*/
		return true;
	}
}