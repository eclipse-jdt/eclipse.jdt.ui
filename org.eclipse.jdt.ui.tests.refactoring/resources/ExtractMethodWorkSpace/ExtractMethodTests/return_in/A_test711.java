package return_in;

public class A_test711 {
	public boolean foo() {
		int i= 0;
		/*[*/switch (i) {
			case 1:
				foo();
			case 2:
				return true;
			default:
				return false;
		}/*]*/
	}
}
