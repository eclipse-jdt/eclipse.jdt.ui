package p;

import java.util.List;

class A{
	/**
	 * @deprecated Use {@link #m(String,int)} instead
	 */
	private void m(int i, String j) {
		m(j, i);
	}

	private void m(String j, int i) {
		List l;
	}
}
