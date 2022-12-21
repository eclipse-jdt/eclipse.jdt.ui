package p; //7, 63, 7, 87

class A {
	public void m(String s) {
		if (s != null && s.length() >= 2) {
			if (s.length() % 2 == 0)
				System.out.println(s.charAt(s.length() - 2) + s.charAt(s.length() - 1));
			if (s.length() % 2 == 1)
				System.out.println(s.charAt(s.length() - 1) + s.charAt(s.length() - 2));
		} else if (s != null && s.length() == 1)
			System.out.println(s.charAt(s.length() - 1));
		return;
	}
}
