package p;
class B{

	public int val() {
		A a= new A(10);
		java.util.function.ToDoubleFunction<A> f = A::g;
		return a.g();
	}
}