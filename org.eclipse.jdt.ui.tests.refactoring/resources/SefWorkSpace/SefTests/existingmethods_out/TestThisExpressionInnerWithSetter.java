package base_out;

public class TestThisExpressionInner {
	private int field;

	class Inner {
		int field;
		public void foo() {
			field= 10;
			TestThisExpressionInner.this.setField(11);
		}
	}
	
	public void foo() {
		setField(10);
	}
	
	int getField() {
		return field;
	}

	public void setField(int newField){
		field=newField;
	}
}
