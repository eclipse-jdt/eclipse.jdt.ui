package p;
class A {
	public static final int Y= 0;
	public static final int X= 0;
}
class Test{
	void f(){
		A a= null;
		int i= a.X;
		u(a);

		A a1= null;
		int i1= a.X;
		u1(a1);
	}
	void u(A a){
		int u= a.X;
	}
	void u1(A a){
		int u= a.Y;
	}
}