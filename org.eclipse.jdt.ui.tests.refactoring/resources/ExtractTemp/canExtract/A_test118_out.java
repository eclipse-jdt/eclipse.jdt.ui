package p; //8, 28, 8, 38

class A {
	public void m(int[] arr) {
		if (arr == null || arr.length == 0) {
			return;
		}
		int length= arr.length;
		for (int i= 0; i < length; ++i) {
			System.out.println(arr[length - i]);
		}
	}
}
