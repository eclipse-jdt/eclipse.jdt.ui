package p;

class Outer{
	class A {
		A(int b, int a){}
	}
}	
class Outer2 extends Outer.A{
	Outer2(){
		new Outer().super(1, 4);
	}
}