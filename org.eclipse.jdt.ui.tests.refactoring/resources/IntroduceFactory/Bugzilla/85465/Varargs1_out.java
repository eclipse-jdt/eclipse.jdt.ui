package p;

public class Varargs1 {
	public static Varargs1 createVarargs1(int... es) {
		return new Varargs1(es);
	}

	private /*[*/Varargs1/*]*/(int... es) { }

	void method() {
		Varargs1 v1= createVarargs1(1, 2, 3);
	}
}
