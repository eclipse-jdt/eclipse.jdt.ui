package locals_out;

public class A_test570 {

	public void foo() {
		Object[] a= null;
		int i= 0;
		for(Object element: a) {
			i = extracted(i);
		}
	}

	protected int extracted(int i) {
		/*[*/i++;/*]*/
		return i;
	}
}

