package trycatch16_in;

public class TestBug566949_2 {
	public void foo(Object o) throws Exception {

		try {
			/*[*/
			if (!(o instanceof TestBug566949_2 x )) {
				throw new Exception();
			} /*]*/
			int i = 4;
			System.out.println(x);
			System.out.println(i);
		} catch (Exception e) {
		}

	}
}