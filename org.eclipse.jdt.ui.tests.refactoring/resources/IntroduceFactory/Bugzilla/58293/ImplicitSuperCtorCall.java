package p;

public class ImplicitSuperCtorCall {
	public /*[*/ImplicitSuperCtorCall/*]*/() {
	}
	public static void main(String[] args) {
		System.out.println("Hello world");
		ImplicitSuperCtorCall iscc= new ImplicitSuperCtorCall();
	}
}

class B extends ImplicitSuperCtorCall {
}
