package expression_out;

public class A_test606 {

	public void foo() {
		int i= 0;
		foo();
		do {
			foo();
		} while (extracted(i));
	}

	protected boolean extracted(int i) {
		return /*[*/i <= 10/*]*/;
	}	
}
