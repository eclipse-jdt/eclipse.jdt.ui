package nameconflict_out;

public class TestFieldInType {
	public void main() {
		int x= 10;
		class T {
			int x;
		}
	}
	
	public void foo() {
		int x= 10;
	}
}
