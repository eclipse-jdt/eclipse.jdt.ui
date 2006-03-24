package p;
abstract class A{

	public abstract int[] m()[];
}
class B extends A{
	@Override
	public int[] m()[] {
		return null;
	}
}