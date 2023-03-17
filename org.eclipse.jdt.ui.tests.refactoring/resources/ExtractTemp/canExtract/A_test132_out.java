package p; // 5, 16, 5, 28

public class A {
	int m(int[] arr, int index) {
		int i= arr[++index];
		return i + arr[++index];
	}
}