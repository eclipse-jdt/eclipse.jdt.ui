package p;

public class CopyModifierAnnotationsParameter {
	@Deprecated
	public final int test;
	public String test2;
	public Integer test4;
	public CopyModifierAnnotationsParameter(int test) {
		this.test = test;
	}
}