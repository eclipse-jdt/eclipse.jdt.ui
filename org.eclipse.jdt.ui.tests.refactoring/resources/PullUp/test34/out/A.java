package p;
abstract class A{

	public abstract void m();
}
class B extends A{
	B(){
		super();
	}
	@Override
	public void m(){}
}