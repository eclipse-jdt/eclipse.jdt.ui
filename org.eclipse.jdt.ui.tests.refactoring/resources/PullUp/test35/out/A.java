package p;
abstract class A{
	A(int i){
		this();
	}
	A(){}
	public abstract void m();
}
class B extends A{
	B(){
		super();
	}
	@Override
	public void m(){}
}