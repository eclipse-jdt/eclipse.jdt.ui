package p;

public class A1 extends A{
	protected A f(){
		return this;
	}
	void test(){
		f().m();
	}
}