package p;
class A implements I {
	public static final int Y= 0;
}
class Test{
	void f(){
		int i= I.X;
		i= A.Y;
	}
}