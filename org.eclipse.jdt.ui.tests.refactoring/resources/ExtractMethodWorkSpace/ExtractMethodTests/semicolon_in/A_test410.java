package semicolon_in;

public class A_test410 {
	public void foo() {
		/*[*/switch (10) {
			case 1:
				foo();
		}/*]*/
	}
}