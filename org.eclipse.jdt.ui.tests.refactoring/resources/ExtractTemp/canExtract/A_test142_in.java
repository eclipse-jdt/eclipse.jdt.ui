package p; // 9, 42, 9, 48

public class A {
	static public int foo(float[] arr) {
		int sum = 0;
		for (int i = 0; i < arr.length - 1; ++i) {
			if (arr[i] * arr[i + 1] < 0) {
				i = i + 1;
				System.out.println((int) arr[i] * (int) arr[i++]);
			} else if (arr[i] < 0) {
				sum -= arr[i];
			} else if (arr[i] > 0) {
				sum += arr[i];
			}
		}
		return sum;
	}

}
