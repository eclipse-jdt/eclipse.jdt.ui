package parameterName_out;

public class A_test906 {
	public void foo() {
		int i = 2;
		/*]*/i = extracted(i);/*[*/
		int a = i;
	}

	protected int extracted(int xxx) {
		xxx = xxx * 2;
		return xxx;
	}
}