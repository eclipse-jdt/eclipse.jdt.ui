package p;

class A1 implements A{
	public void m(boolean b, int i){
	}
	private void foo(){
		m(true, 2);
	}
}
class B extends A1 implements AA{
	public void m(boolean b, int j){
		m(false, 6);
		super.m(true, 4);
	}
}
interface A {
	public void m(boolean b, int i);
}
interface AA {
	public void m(boolean yyy, int xxx);
}