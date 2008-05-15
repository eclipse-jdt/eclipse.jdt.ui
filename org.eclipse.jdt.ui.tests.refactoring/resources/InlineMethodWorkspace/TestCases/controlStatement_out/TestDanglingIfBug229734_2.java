package controlStatement_out;

public class TestDanglingIf {
	boolean a, b;

	void toInline() {
		label: for (;;)
			if (a)
				hashCode();
	}

	void m() {
		if (b) {
			label: for (;;)
			if (a)
				hashCode();
		} else
			toString();
	}
}
