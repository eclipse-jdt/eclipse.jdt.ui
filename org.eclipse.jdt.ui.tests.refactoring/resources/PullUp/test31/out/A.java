package p;
abstract class A{

	public abstract boolean m(int[] a) throws Exception;
}
class B extends A{
	@Override
	public boolean m(int[] a) throws Exception {
		return true;
	}
}
class B1 extends B{
}
abstract class C extends A{
}
class D extends C{

	@Override
	public boolean m(int[] a) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}
}
class D1 extends C{

	@Override
	public boolean m(int[] a) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}
}
class E extends D{
}