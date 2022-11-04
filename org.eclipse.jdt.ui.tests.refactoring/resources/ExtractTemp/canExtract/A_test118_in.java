package p; //8, 28, 8, 38

class A {
	public void m(int[] arr) {
		if (arr == null || arr.length == 0) {
			return;
		}
		for (int i= 0; i < arr.length; ++i) {
			System.out.println(arr[arr.length - i]);
		}
	}
}
