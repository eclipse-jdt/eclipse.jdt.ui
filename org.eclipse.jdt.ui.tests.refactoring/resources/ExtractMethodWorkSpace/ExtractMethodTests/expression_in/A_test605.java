package expression_in;

public class A_test605 {

	public void foo() {
		int i= 0;
		while (/*[*/i <= 10/*]*/)
			foo();
		foo();	
	}	
}
