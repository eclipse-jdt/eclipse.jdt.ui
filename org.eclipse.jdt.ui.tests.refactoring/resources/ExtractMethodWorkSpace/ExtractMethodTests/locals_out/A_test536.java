package locals_out;

public class A_test536 {

	public void bar() {
		{ int k= 27; k++; }
		int i= 37;
		if (i == 0) {
			int k= 17;
			i = extracted(i, k);
		}
		i++;
	}

	protected int extracted(int i, int k) {
		/*[*/k= k + 1;
		k += 2;
		i += 2;
		k++;/*]*/
		return i;
	}
}
