package expression_out;

public class A_test607 {

	public void foo() {
		for (int i= 0;extracted(i); i++)
			foo();
	}

	protected boolean extracted(int i) {
		return /*[*/ i < 10/*]*/;
	}	
}
