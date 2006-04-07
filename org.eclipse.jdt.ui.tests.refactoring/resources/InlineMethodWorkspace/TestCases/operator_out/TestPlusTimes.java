package operator_out;

public class TestPlusTimes {
	int result;
	
	public void foo() {
		result= /*]*/3 * (10 + 10)/*[*/;
	}
	
	public int inline(int x) {
		return 3 * x;
	}
}