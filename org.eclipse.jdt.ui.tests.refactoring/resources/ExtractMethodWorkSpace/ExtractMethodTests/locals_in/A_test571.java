package locals_in;

public class A_test571 {

	public void foo() {
		int i= 0;
		for(Object element: getArray(i++)) {
			/*[*/i= 10;/*]*/
		}
	}
	
	private Object[] getArray(int i) {
		return null;
	}
}

