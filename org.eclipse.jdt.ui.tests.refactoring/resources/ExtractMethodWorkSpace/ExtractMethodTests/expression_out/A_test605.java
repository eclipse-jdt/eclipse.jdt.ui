package expression_out;

public class A_test605 {

	public void foo() {
		int i= 0;
		while (extracted(i))
			foo();
		foo();	
	}

	protected boolean extracted(int i) {
		return /*[*/i <= 10/*]*/;
	}	
}
