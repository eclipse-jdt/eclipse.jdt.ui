package p;

class Outer{
	class A {
		A(int a, int b){}
	}
}	
class Outer2 extends Outer.A{
	Outer2(){
		new Outer().super(4, 1);
	}
}