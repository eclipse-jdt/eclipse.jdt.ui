package p;

class A{
}
class B extends Inner{
	B(){
		super(new A());
	}
}