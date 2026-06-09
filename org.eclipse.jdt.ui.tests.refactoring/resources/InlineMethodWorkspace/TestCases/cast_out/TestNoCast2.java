package cast_in;

public class TestNoCast2 {
	public static void main(String[] args) {
		new TestNoCast2().test();
	}

	void test() {
		/*]*/System.out.println(1); // inline this call
	}

	<T> void print(T item) { // inline this method
		System.out.println(item);
	}
}