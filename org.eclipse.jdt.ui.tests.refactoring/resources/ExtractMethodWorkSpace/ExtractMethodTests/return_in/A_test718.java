package return_in;

public class A_test718 {
	public boolean foo() {
		int i= 0;
		/*[*/switch(i) {
			case 10:
				throw new NullPointerException();	
			default:
				return false;
		}/*]*/
	}
}
