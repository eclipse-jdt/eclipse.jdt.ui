public class A {
	static int sField;
	int field;
	
	public void foo() {
		field= 10;
		this.field= 10;
		A.sField= 10;
		new A().field= 30;
	}
}
