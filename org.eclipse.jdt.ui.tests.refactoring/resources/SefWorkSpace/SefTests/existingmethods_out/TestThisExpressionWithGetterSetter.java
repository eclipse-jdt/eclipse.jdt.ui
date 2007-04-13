package base_out;

public class TestThisExpression {
	private int field;
	
	public void foo() {
		this.setField(10);
		new TestThisExpression().setField(11);
	}
	
	public void setField(int newValue){
		this.field=newValue;
	}
	
	public int getField(){
		return field;
	}
}
