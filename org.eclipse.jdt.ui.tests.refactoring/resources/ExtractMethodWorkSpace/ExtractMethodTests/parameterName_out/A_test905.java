package parameterName_out;

public class A_test905 {
	public void foo() {
		String x = "x";
		extracted(x);
	}

	protected void extracted(String message) {
		/*[*/String y = "a" + message;
		System.out.println(message);/*]*/
	}
}
