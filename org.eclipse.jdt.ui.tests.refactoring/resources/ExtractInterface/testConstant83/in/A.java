package p;
class A {
	public static final int X= 0;
	public static final int Y= 0;
}
class Test{
	void f(A a){
		int i= a.X;
		i= a.Y;
	}
}