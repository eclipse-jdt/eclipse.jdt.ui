package generic_in;

public class TestMethodInstance3 {
	public <T> void toInline(T param) {
		T t= null;
	}
}

class TestMethodInstance3References {
	void bar() {
		TestMethodInstance3 var= null;
		var.toInline("Eclipse");
	}
	void baz() {
		TestMethodInstance3 var= null;
		var.toInline(Boolean.TRUE);
	}
}