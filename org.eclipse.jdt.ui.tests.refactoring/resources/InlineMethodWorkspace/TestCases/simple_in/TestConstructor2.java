package simple_in;

public class TestConstructor2 {
	
	private Object object;

	public TestConstructor2(Object o) {
		object= o;
	}

	public TestConstructor2(Object o, int i) {
		this(o);
	}
}
