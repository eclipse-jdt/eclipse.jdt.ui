package return_in;

public class A_test710 {
	public boolean foo() {
		int i= 0;
		/*[*/switch (i) {
			case 1:
			case 2:
				return true;
			default:
				return false;
		}/*]*/
	}
}
