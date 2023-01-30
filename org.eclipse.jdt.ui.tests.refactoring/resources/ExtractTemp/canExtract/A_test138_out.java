package p; // 11, 14, 11, 35
class A {
	String f(Integer v) {
		String s1 = null;
		String s2 = null;
		String valueOf= String.valueOf(1 + v);
		if (v > 0) {
			s1 = valueOf;
		} else {
			s1 = String.valueOf(v);
		}
		s2 = valueOf;
		return s1 + ", " + s2;
	}
}
