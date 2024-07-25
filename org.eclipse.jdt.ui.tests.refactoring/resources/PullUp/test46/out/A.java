package p;
interface A{

	int CONST = 0;

	int getConst();
}
class B implements A {

	@Override
	public int getConst() {
		return CONST;
	}
}