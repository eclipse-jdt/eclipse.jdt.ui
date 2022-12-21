package p; //8, 32, 8, 42

class A {
	public void foo(String s) {
		if (s == null || s.length() == 0) {
			return;
		} else {
			System.out.println(s.length());
		}
		int z = s.length();
		System.out.println(z);

	}
}
