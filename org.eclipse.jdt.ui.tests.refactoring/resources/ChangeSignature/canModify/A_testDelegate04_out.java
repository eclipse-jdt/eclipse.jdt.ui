package p;

import java.util.List;

class A{
	private void m() {
	}

	/**
	 * @deprecated Use {@link #m()} instead
	 */
	private void m(List l) {
		m();
	}
}
