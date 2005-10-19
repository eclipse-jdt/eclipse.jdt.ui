package p;
abstract class A<T>{
	/**
	 * comment
	 */
	public abstract T m();
}
class B extends A<String>{

	@Override
	public String m() {return null;}
}
class B1 extends B{
}
class C extends A<Object>{

	@Override
	public Object m() {return null;}
}