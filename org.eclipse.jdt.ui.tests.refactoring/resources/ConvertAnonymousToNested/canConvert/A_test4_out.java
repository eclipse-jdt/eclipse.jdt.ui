package p;
//private, static, final
class A{
	private static final class Inner extends A{}
	A(){
	}
	void f(){
		new Inner();
	}
}