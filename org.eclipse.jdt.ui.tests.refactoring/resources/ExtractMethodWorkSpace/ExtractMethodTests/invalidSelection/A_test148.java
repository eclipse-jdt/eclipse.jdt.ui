package invalidSelection;

public class A_test148 {
	boolean flag;
	public boolean foo() {
		int i= 0;
		/*]*/switch (i) {
			case 1:
				return false;
			case 2:
				return true;
		}/*[*/
		return false;
	}
}