package p; // 11, 14, 11, 35
class A {
	String f(Integer v) {
		String s1 = null;
		String s2 = null;
		if (v > 0) {
			s1 = String.valueOf(1 + v);
		} else {
			s1 = String.valueOf(v);
		}
		s2 = String.valueOf(1 + v);
		return s1 + ", " + s2;
	}
}
