package p;
class F{
	class A{
		A(){
		}
	}
}
class X extends F.A{
	X(){
		new F().super();
		new F().new A();
		new F().new A(){};
		new F(){}.new A(){};
	}
}