package p; //6, 9, 6, 25

class A {
	void m() {
		System.out.println(calculateCount());
		calculateCount();
	}
	private static int cnt=1;
	private int calculateCount() {
		cnt++;
		return cnt;
	}
}
