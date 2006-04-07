package operator_out;

public class TestPostfixPlus {
	int result;
	
	public void foo() {
		int i= 10;
		result= /*]*/1 + i++/*[*/;
	}
	
	public int inline(int x) {
		return 1 + x;
	}
}