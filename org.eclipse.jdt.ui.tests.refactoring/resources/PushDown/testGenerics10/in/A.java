package p;
class A<T>{
	/**
	 * comment
	 */
	public void m() {}
}
class B<T> extends A<T>{
}
class B1 extends B<String>{
}
class C extends A<String>{
}