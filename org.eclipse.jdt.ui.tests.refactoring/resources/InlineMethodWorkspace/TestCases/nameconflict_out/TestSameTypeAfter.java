package nameconflict_out;

public class TestSameTypeAfter {
	public void main() {
		class T1 {
			T1 t;
			public T1() {}
		}
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
