package return_in;
public class A_test719 {
	public boolean foo() {
		/*[*/if (foo())
			return true;
		if (foo())
			return true;
		else
			return false;/*]*/
	}
}

