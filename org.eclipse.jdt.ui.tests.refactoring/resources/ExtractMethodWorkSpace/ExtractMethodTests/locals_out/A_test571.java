package locals_out;

public class A_test571 {

	public void foo() {
		int i= 0;
		for(Object element: getArray(i++)) {
			extracted();
		}
	}

	protected void extracted() {
		int i;
		/*[*/i= 10;/*]*/
	}
	
	private Object[] getArray(int i) {
		return null;
	}
}

