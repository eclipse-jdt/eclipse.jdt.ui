package p;

class A {
	void f() {
		StringBuffer buf= new StringBuffer();
		String[] strings= new String[] {"A", "B", "C", "D"};
		final int temp= strings.length;
		for (int i= 0; i < temp; i++) {
			buf.append(strings[i]);
		}
	}
}
