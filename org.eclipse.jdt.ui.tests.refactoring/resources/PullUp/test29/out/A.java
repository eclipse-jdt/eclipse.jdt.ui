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
class C extends A{

	@Override
	public boolean m(int[] a) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}
}