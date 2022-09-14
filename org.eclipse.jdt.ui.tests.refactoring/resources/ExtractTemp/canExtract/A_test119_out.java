package p; //5, 28, 8, 38

class A {
	public int m(int[] arr) {
		int length= arr.length;
		if (arr == null || length == 0) {
			return -1;
		}
		return arr[0];
	}
}