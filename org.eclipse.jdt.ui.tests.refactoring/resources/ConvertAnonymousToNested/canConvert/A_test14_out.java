package p;
//private, static, final
class A{
	private final class Inner {
	}

	void f(){
		new Inner();
	}
}