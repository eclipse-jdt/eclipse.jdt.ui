package duplicates_in;

// don't extract second occurence of
// 2 since it is in a inner class
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
