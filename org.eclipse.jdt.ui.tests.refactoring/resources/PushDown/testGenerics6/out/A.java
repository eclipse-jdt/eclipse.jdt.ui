package p;
abstract class A<T>{
	public abstract T m();
}
abstract class B<S> extends A<S>{

	public abstract S m();
}