package nameconflict_in;

public class TestLocalInType {
	public void main() {
		/*]*/foo();/*[*/
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
