package object_out;

public class TestCompoundWrite3 {
	TestCompoundWrite3 a;
	private String field;
	
	public void foo() {
		a.a.setField(a.a.getField() + "d");
	}

	void setField(String field) {
		this.field = field;
	}

	String getField() {
		return field;
	}
}
