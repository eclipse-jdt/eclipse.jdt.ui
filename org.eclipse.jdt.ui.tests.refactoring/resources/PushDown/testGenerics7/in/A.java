package p;
abstract class A{
	public abstract void m();
}
abstract class B<S> extends A{
}
abstract class B1<S> extends B<String>{
}
abstract class C<S> extends A{
}