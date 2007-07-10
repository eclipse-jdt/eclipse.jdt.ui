package p;

public class B {
	public static void main(String[] args) {
		I[] arr = new I[5];
		for(I mytest : arr) {
			mytest.m1();
		}
	}
}