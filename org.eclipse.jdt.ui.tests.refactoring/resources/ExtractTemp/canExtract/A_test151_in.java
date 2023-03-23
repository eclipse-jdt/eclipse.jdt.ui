package p; // 8, 17, 8, 20

public class A {
	public void foo(int k) {
		f();
		switch (k) {
			case 3:
				f();
				int x= f();
				break;
			default:
				int y= f();
		}
	}

	int f() {
		return 1;
	}
}
