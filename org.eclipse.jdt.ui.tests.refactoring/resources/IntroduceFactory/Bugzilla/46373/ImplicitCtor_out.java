package p;

public class ImplicitCtor {
	public static ImplicitCtor createImplicitCtor() {
		return new ImplicitCtor();
	}
	public void foo() {
		createImplicitCtor();
	}
	public void bar() {
		createImplicitCtor();
	}
}
