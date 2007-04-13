package base_in;

public class TestThisExpressionInner {
	int field;

	class Inner {
		int field;
		public void foo() {
			field= 10;
			TestThisExpressionInner.this.field= 11;
		}
	}
	
	public void foo() {
		field= 10;
	}
	
	public void setField(int newField){
		field=newField;
	}
}
