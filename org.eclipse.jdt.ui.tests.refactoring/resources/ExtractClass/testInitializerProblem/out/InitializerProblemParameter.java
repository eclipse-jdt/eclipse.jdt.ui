package p;

import java.util.ArrayList;

public class InitializerProblemParameter {
	public int test;
	public int test2 = test;
	public ArrayList list;
	public InitializerProblemParameter(int test, ArrayList list) {
		this.test = test;
		this.list = list;
	}
}