package locals_in;

public class A_test536 {

	public void bar() {
		{ int k= 27; k++; }
		int i= 37;
		if (i == 0) {
			int k= 17;
			/*[*/k= k + 1;
			k += 2;
			i += 2;
			k++;/*]*/
		}
		i++;
	}
}
