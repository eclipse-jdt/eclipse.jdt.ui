package p;

public class ImplicitSuperCtorCall {
	protected /*[*/ImplicitSuperCtorCall/*]*/() {
	}
	public static void main(String[] args) {
		System.out.println("Hello world");
		ImplicitSuperCtorCall iscc= createImplicitSuperCtorCall();
	}
	public static ImplicitSuperCtorCall createImplicitSuperCtorCall() {
		return new ImplicitSuperCtorCall();
	}
}

class B extends ImplicitSuperCtorCall {
}
