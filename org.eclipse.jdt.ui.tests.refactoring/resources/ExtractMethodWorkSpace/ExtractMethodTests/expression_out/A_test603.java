package expression_out;

public class A_test603 {

	public void foo() {
		int i= 10;
		if (extracted(i))
			foo();
	}

	protected boolean extracted(int i) {
		return /*[*/i == 10/*]*/;
	}	
}
