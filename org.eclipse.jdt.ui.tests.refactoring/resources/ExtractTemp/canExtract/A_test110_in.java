package p; //6, 9, 6, 25

class A {
	void m() {
		System.out.println(calculateCount());
		calculateCount();
	}
	private int calculateCount() {
		return 1;
	}
}
