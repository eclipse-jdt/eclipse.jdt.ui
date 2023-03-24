package p; // 14, 17, 14, 38

public class A {
	public static int commonBinarySearch(int[] arr, int key) {
		int low = 0;
		int high = arr.length - 1;
		if (key < arr[low] || key > arr[high] || low > high) {
			return -1;
		}
		if (arr[(low + high) / 2] == key) {
			return (low + high) / 2;
		}
		while (low <= high && arr[(low + high) / 2] != key) {
			if (arr[(low + high) / 2] > key) {
				high = (low + high) / 2 - 1;
			} else if (arr[(low + high) / 2] < key) {
				for (; isNoOutOfBound(arr, (low + high) / 2) && arr[(low + high) / 2] < key; ++low)
					;
			}
		}
		if (isNoOutOfBound(arr, (low + high) / 2))
			return arr[(low + high) / 2];
		else
			return -1;
	}

	static boolean isNoOutOfBound(int[] arr, int index) {
		return index >= 0 && index < arr.length;
	}
}
