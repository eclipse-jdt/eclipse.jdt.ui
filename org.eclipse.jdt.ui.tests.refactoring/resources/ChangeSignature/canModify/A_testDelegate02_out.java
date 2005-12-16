package p;

import java.util.List;

class A{
	private void m(List list) {
		m(list);
	}

	/**
	 * @deprecated Use {@link #m(List)} instead
	 */
	private void m() {
		m(null);
	}
}
