package p;
abstract class A<T>{
	public abstract void m();
}
class B extends A<String>{

	@Override
	public void m() {}
}