package validSelection;

public class A_test190_ {
	public int foo() {
		int i= 10;
		/*]*/switch(i) {
			case 1:
				foo();
				break;
			case 2:
				foo();
			default:
				return 10;
		}/*[*/
		
		return 20;
	}
}