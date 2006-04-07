package operator_out;

public class TestPlusPostfix {
	int result;
	
	public void foo() {
		int i= 10;
		int x = 10 + 10;
		result= /*]*/x++/*[*/;
	}
	
	public int inline(int x) {
		return x++;
	}
}