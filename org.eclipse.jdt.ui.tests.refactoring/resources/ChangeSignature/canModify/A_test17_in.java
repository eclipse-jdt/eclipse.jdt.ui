package p;

class A1 implements A{
	public void m(int i, boolean b){
	}
	private void foo(){
		m(2, true);
	}
}
class B extends A1{
	public void m(int j, boolean b){
		m(6, false);
		super.m(4, true);
	}
}
interface A {
	public void m(int i, boolean b);
}