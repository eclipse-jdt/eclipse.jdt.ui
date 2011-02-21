package operator_out;

public class TestPlusPlus_1 {
	double result;
	
	public void foo() {
		result= /*]*/1.1 + (10.1 + 10.1)/*[*/;
	}
	
	public double inline(double x) {
		return 1.1 + x;
	}
}