package argument_in;

public class TestArray {

	public int bar(int a[]) {
		return a[0];
	}
	
	public void main() {
		int i= /*]*/bar(new int[] {1})/*[*/;
	}
}
