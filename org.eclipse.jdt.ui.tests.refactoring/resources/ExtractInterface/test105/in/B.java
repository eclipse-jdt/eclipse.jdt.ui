package p;

public class B {
	public static void main(String[] args) {
		A[] arr = new A[5];
		for(A mytest : arr) {
			mytest.m1();
		}
	}
}