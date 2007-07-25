package p;

public class InitializerProblemParameter {
	public int test;
	public int test2 = test;
	public InitializerProblemParameter(int test) {
		this.test = test;
	}
}