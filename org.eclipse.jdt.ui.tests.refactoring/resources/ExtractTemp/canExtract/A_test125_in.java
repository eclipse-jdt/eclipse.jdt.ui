package p; //9, 32, 9, 42

class A {
	public void foo(String s) {
		if (s == null || s.length() == 0) {
			return;
		} else {
			int length2 = 3;
			System.out.println(s.length());
			System.out.println(length2);
		}
		int z = s.length();
		System.out.println(z);

	}
}
