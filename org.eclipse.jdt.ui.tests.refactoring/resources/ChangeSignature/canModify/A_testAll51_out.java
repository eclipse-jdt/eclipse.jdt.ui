package p;
class F{
	class A{
		A(int i){
		}
	}
}
class X extends F.A{
	X(){
		new F().super(1);
		new F().new A(1);
		new F().new A(1){};
		new F(){}.new A(1){};
	}
}