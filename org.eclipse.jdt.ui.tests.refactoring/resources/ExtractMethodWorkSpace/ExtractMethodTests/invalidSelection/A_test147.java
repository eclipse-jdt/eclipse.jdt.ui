package invalidSelection;

public class A_test147 {
	boolean flag;
	public boolean foo() {
		/*]*/target: {
			for (int i= 0; i < 10; i++) {
				if (flag)
					break;
				else
					break target;
			}
			return false;
		}/*[*/
		return true;
	}
}