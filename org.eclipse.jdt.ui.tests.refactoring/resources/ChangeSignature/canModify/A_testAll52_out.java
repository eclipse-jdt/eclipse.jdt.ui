package p;
class A{
	A(int i){
	}
}
class B extends A{
	B(){
		this(3);
	}
	B(int y){
		super(1);
	}
}