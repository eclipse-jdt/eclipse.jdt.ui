package p;
class A{
	protected void m(int i, boolean b){
	}
	private void foo(){
		m(2, true);
	}
}
class B extends A{
	protected void m(int j, boolean b){
		m(6, false);
		super.m(4, true);
	}
}