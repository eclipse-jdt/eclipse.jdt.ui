package p; // 11, 14, 11, 33
class A {
	String f(Integer v) {
		String s1 = null;
		String s2 = null;
		if (v > 0) {
			s1 = String.valueOf(++v);
		} else {
			s1 = String.valueOf(v);
		}
		String valueOf= String.valueOf(++v);
		s2 = valueOf;
		return s1 + ", " + s2;
	}
}
