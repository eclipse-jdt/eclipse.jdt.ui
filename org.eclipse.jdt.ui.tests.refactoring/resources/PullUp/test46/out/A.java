package p;
interface A{

	public static final int CONST = 0;

	public int getConst();
}
class B implements A {

	public int getConst() {
		return CONST;
	}
}