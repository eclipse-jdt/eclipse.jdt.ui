package p;
class A{
	void f(int a, int b)
	{
	}
}
class B extends A{
	void g() {
		super.f(1,2);
	}
}
