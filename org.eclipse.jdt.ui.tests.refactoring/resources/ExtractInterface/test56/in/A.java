package p;

public class A {
	public void m() {}
	public void m1() {}
	protected A f(){
		return this;
	}
	void test(){
		f().m1();
	}
}