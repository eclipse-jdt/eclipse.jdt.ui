package invalid;

public class TestComplexBody {
	public void main() {
		int i= 10 + /*]*/foo()/*[*/;
	}
	
	public int foo() {
		int i= 20;
		return i + 2;
	}
}
