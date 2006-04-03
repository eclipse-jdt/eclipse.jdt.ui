package locals_in;

public class A_test578 {
	public int foo(boolean b1, boolean b2) {
		int n = 0;
		int i = 0;
		/*[*/
		if (b1)
			i = 1;
		if (b2)
			n = n + i;
		/*]*/
		return n;
	}
}

