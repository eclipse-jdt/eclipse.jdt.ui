package p;
interface A{

	int[] m()[];
}
class B implements A{
	@Override
	public int[] m()[] {
		return null;
	}
}