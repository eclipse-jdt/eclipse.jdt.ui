package invalidSelection;

public class A_test149 {
	boolean flag;
	public boolean foo() {
		int i= 0;
		/*]*/switch (i) {
			case 1:
				break;
			case 2:
				return true;
			default:
				return false;
		}/*[*/
		return false;
	}
}