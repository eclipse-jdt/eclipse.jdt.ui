package expression_in;

public class A_test606 {

	public void foo() {
		int i= 0;
		foo();
		do {
			foo();
		} while (/*[*/i <= 10/*]*/);
	}	
}
