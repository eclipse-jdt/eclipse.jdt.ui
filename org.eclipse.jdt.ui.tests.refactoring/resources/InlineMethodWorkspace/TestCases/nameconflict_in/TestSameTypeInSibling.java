package nameconflict_in;

public class TestSameTypeInSibling {
	public void main() {
		class T {
			public T() {}
		}
		int x= 10;
		/*]*/foo();/*[*/
	}
	
	public void foo() {
		class T {
			T t;
			public T() {}
		}
		class X {
			T t;
			void foo() {
				int x;
				T t;
			}
		}
	}
}
