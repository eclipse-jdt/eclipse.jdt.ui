package nameconflict_in;

public class TestFieldInType {
	public void main() {
		/*]*/foo();/*[*/
		class T {
			int x;
		}
	}
	
	public void foo() {
		int x= 10;
	}
}
