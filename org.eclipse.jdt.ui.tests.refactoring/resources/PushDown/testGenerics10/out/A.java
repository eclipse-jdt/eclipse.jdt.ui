package p;
class A<T>{
}
class B<T> extends A<T>{

	/**
	 * comment
	 */
	public void m() {}
}
class B1 extends B<String>{
}
class C extends A<String>{

	/**
	 * comment
	 */
	public void m() {}
}