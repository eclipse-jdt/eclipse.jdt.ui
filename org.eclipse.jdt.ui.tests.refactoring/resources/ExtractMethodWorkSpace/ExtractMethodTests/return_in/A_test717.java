package return_in;

public class A_test717 {
	public boolean foo() {
		int i= 0;
		/*[*/switch(i) {
			case 10:
				return false;
			default:
				throw new NullPointerException();	
		}/*]*/
	}
}
