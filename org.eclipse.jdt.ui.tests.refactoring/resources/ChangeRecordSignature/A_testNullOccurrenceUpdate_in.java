package p;

import java.util.function.Supplier;

record A(int a, String b){
}
class B {
	protected void m(){
		A a = new A(1, "string");
		Supplier<A> s = A::new;
	}
}
