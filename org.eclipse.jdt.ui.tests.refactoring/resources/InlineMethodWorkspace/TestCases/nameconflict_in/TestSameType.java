package nameconflict_in;

public class TestSameType {
	public void main() {
		class T {
			public T() {}
		}
		/*]*/foo();/*[*/
	}
	
	public void foo() {
		class T {
			T t;
			public T() {}
		}
	}
}
