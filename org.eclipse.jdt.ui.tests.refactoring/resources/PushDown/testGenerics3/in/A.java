package p;
class A<S>{
	public void m() {}
}
class B<T> extends A<T>{
}
class B1 extends B<String>{
}
class C<R> extends A<R>{
}