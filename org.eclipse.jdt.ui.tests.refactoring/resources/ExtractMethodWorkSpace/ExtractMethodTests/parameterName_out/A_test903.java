package parameterName_out;

public class A_test903 {
	public void foo() {
		int i= 0;
		float y= 0;
		
		extracted(i, y);
	}

	protected void extracted(int xxx, float yyyy) {
		/*[*/System.out.println(xxx + yyyy);/*]*/
	}
}
