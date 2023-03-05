package p;

class A {
	public static long fibonacci(long number) {
		if ((number == 0) || (number == 1)) {
			System.out.println("stop recursion");
			return number;
		} else {
			long fibonacci= fibonacci(number - 1);
			return fibonacci(number - 1) + fibonacci(number - 2);
		}
	}
}
