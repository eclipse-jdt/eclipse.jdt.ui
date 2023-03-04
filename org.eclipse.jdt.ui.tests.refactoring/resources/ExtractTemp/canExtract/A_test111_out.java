package p; //11, 9, 11, 25

class A {
	void m() {
		System.out.println(calculateCount());
		calculateCount();
		System.out.println(calculateCount());
		
		int x= calculateCount();
		
		int temp= calculateCount();
	}
	private static int cnt=1;
	private int calculateCount() {
		cnt++;
		return cnt;
	}
}
