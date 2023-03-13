package p; //6, 9, 6, 25

class A {
	void m() {
		int temp= calculateCount();
		System.out.println(temp);
		calculateCount();
	}
	private int calculateCount() {
		return 1;
	}
}
