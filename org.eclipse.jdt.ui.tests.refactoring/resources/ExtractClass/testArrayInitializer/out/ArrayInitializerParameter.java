package p;

public class ArrayInitializerParameter {
	private String[] test;
	private int[] val;
	public ArrayInitializerParameter(String[] test, int[] val) {
		this.test = test;
		this.val = val;
	}
	public String[] getTest() {
		return test;
	}
	public void setTest(String[] test) {
		this.test = test;
	}
	public int[] getVal() {
		return val;
	}
	public void setVal(int[] val) {
		this.val = val;
	}
}