package expression_in;

public class A_test601 {

	public void foo() {
		int i= 10;
		if (/*[*/i < 10 && i < 20/*]*/)
			foo();
	}	
}
