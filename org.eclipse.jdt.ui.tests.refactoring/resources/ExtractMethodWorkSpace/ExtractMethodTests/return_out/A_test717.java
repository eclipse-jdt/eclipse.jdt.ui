package return_out;

public class A_test717 {
	public boolean foo() {
		int i= 0;
		return extracted(i);
	}

	protected boolean extracted(int i) {
		/*[*/switch(i) {
			case 10:
				return false;
			default:
				throw new NullPointerException();	
		}/*]*/
	}
}
