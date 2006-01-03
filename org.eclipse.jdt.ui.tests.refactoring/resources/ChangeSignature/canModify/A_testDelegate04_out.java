package p;

import java.util.List;

class A{
	/**
	 * @deprecated Use {@link #m()} instead
	 */
	private void m(List l) {
		m();
	}

	private void m() {
	}
}
