package p;
class A implements I {
	public static final int Y= 0;
}
class Test{
	void f(A a){
		int i= a.X;
		i= a.Y;
	}
}