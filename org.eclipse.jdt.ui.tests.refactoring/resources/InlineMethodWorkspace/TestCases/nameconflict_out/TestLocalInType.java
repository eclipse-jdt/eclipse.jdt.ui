package nameconflict_out;

public class TestLocalInType {
	public void main() {
		int x= 10;
		int bar= 20;
		class T {
			void bar() {
				int x;
			}
		}
	}
	
	public void foo() {
		int x= 10;
		int bar= 20;
	}
}
