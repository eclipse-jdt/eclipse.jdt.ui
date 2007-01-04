package controlStatement_out;

public class TestDanglingIf {
	boolean a, b;

	void toInline() {
		if (a)
			hashCode();
	}

	void m() {
		if (b) {
			if (a)
				hashCode();
		} else
			toString();
	}
}
