package duplicates_in;

public class A_test958 {
	private Object fO;

	public void method0() {
		/*[*/Object o2= fO;/*]*/
		fO= o2;
	}

	public void method1() {
		Object o= fO;
		fO= o;
	}
}
