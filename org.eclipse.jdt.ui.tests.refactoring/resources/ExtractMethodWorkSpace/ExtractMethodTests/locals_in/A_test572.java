package locals_in;

public class A_test572 {

	public void foo() {
		int i= 0;
		while (true) {
			for(Object element: getArray(i)) {
				/*[*/i= 10;/*]*/
			}
		}
	}
	
	private Object[] getArray(int i) {
		return null;
	}
}

