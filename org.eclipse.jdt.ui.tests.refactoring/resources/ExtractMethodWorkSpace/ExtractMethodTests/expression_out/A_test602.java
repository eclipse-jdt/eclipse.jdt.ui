package expression_out;

public class A_test602 {

	public void foo() {
		int i= 10;
		if (extracted(i))
			foo();
	}

	protected boolean extracted(int i) {
		return /*[*/i < 10 || i < 20/*]*/;
	}	
}
