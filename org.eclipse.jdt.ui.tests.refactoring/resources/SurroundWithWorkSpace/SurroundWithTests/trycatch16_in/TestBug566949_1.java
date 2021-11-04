package trycatch16_in;

public class TestBug566949_1 {
	public void foo(Object o) throws Exception {

		/*[*/
		if (!(o instanceof TestBug566949_1 x )) {
			throw new Exception();
		} /*]*/
		System.out.println(x);

	}
}