package p;
abstract class A<T>{
	/**
	 * comment
	 */
	public abstract T m();
}
class B<T> extends A<T>{

	public T m() {return null;}
}