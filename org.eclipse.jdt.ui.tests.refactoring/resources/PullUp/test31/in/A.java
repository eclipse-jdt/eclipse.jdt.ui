package p;
class A{
}
class B extends A{
	public boolean m(int[] a) throws Exception {
		return true;
	}
}
class B1 extends B{
}
abstract class C extends A{
}
class D extends C{
}
class D1 extends C{
}
class E extends D{
}