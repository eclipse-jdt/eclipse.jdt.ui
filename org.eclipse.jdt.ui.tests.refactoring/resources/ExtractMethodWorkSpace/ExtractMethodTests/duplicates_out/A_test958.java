package duplicates_out;

public class A_test958 {
	private Object fO;

	public void method0() {
		Object o2 = extracted();
		fO= o2;
	}

	protected Object extracted() {
		/*[*/Object o2= fO;/*]*/
		return o2;
	}

	public void method1() {
		Object o = extracted();
		fO= o;
	}
}
