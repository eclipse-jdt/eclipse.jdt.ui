package controlStatement_out;

public class TestDanglingIf {
	boolean a, b;

	void toInline() {
		while (true)
			if (a)
				hashCode();
	}

	void m() {
		if (b) {
			while (true)
			if (a)
				hashCode();
		} else
			toString();
	}
}
