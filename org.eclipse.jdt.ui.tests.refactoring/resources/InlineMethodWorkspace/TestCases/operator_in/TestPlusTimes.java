package operator_in;

public class TestPlusTimes {
	int result;
	
	public void foo() {
		result= /*]*/inline(10 + 10)/*[*/;
	}
	
	public int inline(int x) {
		return 3 * x;
	}
}