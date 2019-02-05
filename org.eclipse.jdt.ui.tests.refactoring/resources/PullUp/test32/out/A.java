package p;
abstract class A{

	public abstract int m();
}
class B extends A{
	@Override
	public int m() {
		return 2 +3;
	}
}
class C extends A{

	@Override
	public int m() {
		// TODO Auto-generated method stub
		return 0;
	}
}