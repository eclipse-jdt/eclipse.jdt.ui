package base_in;

public class TestThisExpression {
	int field;
	
	public void foo() {
		this.field= 10;
		new TestThisExpression().field= 11;
	}
	
	public void setField(int newValue){
		this.field=newValue;
	}
	
	public int getField(){
		return field;
	}
}
