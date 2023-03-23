package p; // 10, 17, 10, 20

public class A {
	public void foo(int k) {
		f();
		switch (k) {
			case 3:
				break;
			default:
				int f= f();
				int x= f;
				int y= f;
		}
		int z= f();
	}

	int f() {
		return 1;
	}
}
