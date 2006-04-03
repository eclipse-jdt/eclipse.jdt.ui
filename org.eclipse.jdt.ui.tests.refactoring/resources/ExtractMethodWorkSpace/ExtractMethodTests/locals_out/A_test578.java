package locals_out;

public class A_test578 {
	public int foo(boolean b1, boolean b2) {
		int n = 0;
		int i = 0;
		n = extracted(b1, b2, n, i);
		return n;
	}

	protected int extracted(boolean b1, boolean b2, int n, int i) {
		/*[*/
		if (b1)
			i = 1;
		if (b2)
			n = n + i;
		/*]*/
		return n;
	}
}

