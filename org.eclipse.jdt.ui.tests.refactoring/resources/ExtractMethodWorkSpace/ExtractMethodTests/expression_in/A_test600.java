package expression_in;

public class A_test600 {

	public void foo() {
		int i= 10;
		if (/*[*/i < 10/*]*/)
			foo();
	}	
}
