package p;
class A<T>{
	/**
	 * comment
	 */
	public T m() {return null;}
}
class B extends A<String>{
}
class B1 extends B{
}
class C extends A<Object>{
}