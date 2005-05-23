package invalid;

public class TestMultipleMethods {
	void bar() {
		toInline("");
	}

	public void toInline(String d) {
		System.out.println(d);
	}

	public void toInline(String d) {
		System.out.println("");
	}
}
