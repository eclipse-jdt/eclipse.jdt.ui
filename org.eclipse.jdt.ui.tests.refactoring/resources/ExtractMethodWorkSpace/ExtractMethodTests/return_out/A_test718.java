package return_out;

public class A_test718 {
	public boolean foo() {
		int i= 0;
		return extracted(i);
	}

	protected boolean extracted(int i) {
		/*[*/switch(i) {
			case 10:
				throw new NullPointerException();	
			default:
				return false;
		}/*]*/
	}
}
