package p;
class A{
	protected void m(boolean b, int i){
	}
	private void foo(){
		m(true, 2);
	}
}
class B extends A{
	protected void m(boolean b, int j){
		m(false, 6);
		super.m(true, 4);
	}
}