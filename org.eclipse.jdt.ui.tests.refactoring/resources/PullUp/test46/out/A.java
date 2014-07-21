package p;
interface A{

	int CONST = 0;

	int getConst();
}
class B implements A {

	public int getConst() {
		return CONST;
	}
}