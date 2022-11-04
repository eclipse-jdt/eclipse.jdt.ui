package p; //8, 32, 8, 42

class A {
	public void foo(String s) {
		if (s == null || s.length() == 0) {
			return;
		} else {
			int length= s.length();
			System.out.println(length);
		}
		int length2= s.length();
		int z = length2;
		System.out.println(z);

	}
}
