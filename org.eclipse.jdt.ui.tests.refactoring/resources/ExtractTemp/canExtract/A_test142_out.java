package p; // 9, 42, 9, 48

public class A {
	static public int foo(float[] arr) {
		int sum = 0;
		for (int i = 0; i < arr.length - 1; ++i) {
			float f2= arr[i];
			if (f2 * arr[i + 1] < 0) {
				i = i + 1;
				float f= arr[i];
				System.out.println((int) f * (int) arr[i++]);
			} else {
				float f3= arr[i];
				if (f3 < 0) {
					sum -= f3;
				} else if (f3 > 0) {
					sum += f3;
				}
			}
		}
		return sum;
	}

}
