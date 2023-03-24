package p; // 7, 17, 7, 33

public class A {
	void m(int x) {
		while (x++ < 10) {
			for (int i = 1; i < 10; ++i)
				calculateCount();
			calculateCount();
		}
	}

	private int calculateCount() {
		return 0;
	}
}
