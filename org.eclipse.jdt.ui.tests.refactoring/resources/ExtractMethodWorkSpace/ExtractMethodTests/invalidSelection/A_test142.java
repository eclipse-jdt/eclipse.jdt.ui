package invalidSelection;

public class A_test142 {
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