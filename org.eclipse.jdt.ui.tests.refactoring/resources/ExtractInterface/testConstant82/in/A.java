package p;
class A {
	public static final int X= 0;
	public static final int Y= 0;
}
class Test{
	void f(){
		int i= A.X;
		i= A.Y;
	}
}