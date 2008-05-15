package controlStatement_in;

public class TestDanglingIf {
	boolean a, b;

	void toInline() {
		while (true)
			if (a)
				hashCode();
	}

	void m() {
		if (b)
			/*]*/toInline()/*[*/;
		else
			toString();
	}
}
