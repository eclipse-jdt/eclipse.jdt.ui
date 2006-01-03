package p;

import java.util.List;

class A{
	/**
	 * @deprecated Use {@link #m(List)} instead
	 */
	private void m() {
		m(null);
	}

	private void m(List list) {
		m(list);
	}
}
