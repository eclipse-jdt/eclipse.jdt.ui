package base_out;

public class TestTwoFragments {
	@Deprecated volatile int anotherField;
	@Deprecated
	private volatile int field;
	public int getField(){
		return field;
	}
	void setField(int field) {
		this.field = field;
	}
}
