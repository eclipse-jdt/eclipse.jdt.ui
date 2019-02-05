package p;
abstract class A{

	public abstract int[] m();
}
class B extends A{
	@Override
	public int[] m() {
		return new int[0];
	}
}
class C extends A{

	@Override
	public int[] m() {
		// TODO Auto-generated method stub
		return null;
	}
}