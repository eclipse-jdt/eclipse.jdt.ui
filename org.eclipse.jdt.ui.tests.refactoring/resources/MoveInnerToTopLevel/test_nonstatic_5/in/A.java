package p;

class A{
	class Inner{
	}
}
class B extends A.Inner{
	B(){
		new A().super();
	}
}