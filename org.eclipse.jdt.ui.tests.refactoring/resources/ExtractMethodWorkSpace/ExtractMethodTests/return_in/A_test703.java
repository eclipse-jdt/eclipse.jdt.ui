package return_in;

public class A_test703 {
	public boolean foo() {
		/*[*/if (0 == 0) {
			if (10 == 10)
				return true;
			else
				return false;
		}
		foo();
		return false;/*]*/
	}
}
