package return_out;

public class A_test723 {
	public void foo() {
		{
			int i= 0;
			i = extracted(i);
			i++;
		}
	}

	protected int extracted(int i) {
		/*[*/i--;/*]*/
		return i;
	}
}
