package nameconflict_out;

public class TestSameTypeInSibling {
	public void main() {
		class T {
			public T() {}
		}
		int x= 10;
		class T1 {
			T1 t;
			public T1() {}
		}
		class X {
			T1 t;
			void foo() {
				int x;
				T1 t;
			}
		}
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
