package p;
class A implements I {
	public static final int Y= 0;
}
class Test{
	void f(){
		I a= null;
		int i= a.X;
		u(a);

		A a1= null;
		int i1= a.X;
		u1(a1);
	}
	void u(I a){
		int u= a.X;
	}
	void u1(A a){
		int u= a.Y;
	}
}