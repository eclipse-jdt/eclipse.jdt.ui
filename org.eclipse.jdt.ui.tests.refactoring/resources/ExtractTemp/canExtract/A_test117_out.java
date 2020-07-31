package p; //8, 18, 8, 19

class A {
	private void foo(int i) {
		final int temp= 2;
		switch (i) {
			case 1:
				break;
			case temp:
				break;
		}
	}
}
