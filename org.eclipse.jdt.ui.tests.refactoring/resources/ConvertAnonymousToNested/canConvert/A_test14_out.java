package p;
//private, static, final
class A{
	private static final class Inner{}
	void f(){
		new Inner();
	}
}