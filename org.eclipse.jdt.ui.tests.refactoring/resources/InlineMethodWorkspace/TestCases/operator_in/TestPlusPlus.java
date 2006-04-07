package operator_in;

public class TestPlusPlus {
	int result;
	
	public void foo() {
		result= /*]*/inline(10 + 10)/*[*/;
	}
	
	public int inline(int x) {
		return 1 + x;
	}
}