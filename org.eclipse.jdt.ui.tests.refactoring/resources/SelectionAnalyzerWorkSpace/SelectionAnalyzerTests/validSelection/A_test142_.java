package validSelection;

public class A_test142_ {
	private boolean flag;
	public int foo() {
		int i= 10;
		/*]*/if (flag) {
			i++;
			return i;
		}/*[*/
		int y= i + 10;
		return y;
	}
}