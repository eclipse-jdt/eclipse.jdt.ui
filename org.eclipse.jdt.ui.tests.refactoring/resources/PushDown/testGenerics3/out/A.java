package p;
class A<S>{
}
class B<T> extends A<T>{

	public void m() {}
}
class B1 extends B<String>{
}
class C<R> extends A<R>{

	public void m() {}
}