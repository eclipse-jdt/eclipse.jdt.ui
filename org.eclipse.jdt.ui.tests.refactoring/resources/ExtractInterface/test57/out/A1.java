package p;

public class A1 extends A{
	protected I f(){
		return this;
	}
	void test(){
		f().m();
	}
}