package p; //8, 32, 8, 42

class A {
	public void foo(String s) {
		if (s == null || s.length() == 0) {
			return;
		} else {
			int length= s.length();
			System.out.println(length);
		}
		int length_1= s.length();
		int z = length_1;
		System.out.println(z);

	}
}
