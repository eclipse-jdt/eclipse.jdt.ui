package nameconflict_in;

public class TestSameTypeAfter {
	public void main() {
		/*]*/foo();/*[*/
		class T {
			public T() {}
		}
	}
	
	public void foo() {
		class T {
			T t;
			public T() {}
		}
	}
}
