package object_out;

public class TestCompoundWrite3 {
	TestCompoundWrite3 a;
	private String field;
	
	public void foo() {
		a.a.setField(a.a.getField() + "d");
	}

	String getField() {
		return field;
	}

	void setField(String field) {
		this.field = field;
	}
}
