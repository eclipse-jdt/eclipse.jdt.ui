package nameconflict_out;

public class TestSameType {
	public void main() {
		class T {
			public T() {}
		}
		class T1 {
			T1 t;
			public T1() {}
		}
	}
	
	public void foo() {
		class T {
			T t;
			public T() {}
		}
	}
}
