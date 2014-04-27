// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestInlineMethodRef_Cast {
	public static final FI fi = TestInlineMethodRef_Cast::m;
	
	private static int m(int x) {
		return x++;
	}

	private FI fun3() {
		F a = o -> null;
		return a.bar(fi); // [1]
	}

	private Object fun5() {
		Object o = fi; // [2]
		F fx = (z) -> {
			z = fi; // [3]
			return null;
		};

		Object fi2;
		fi2 = fi; // [4]

		Object[] b = new Object[] { fi, fi }; // [5]
		Object[][] c = new Object[][] { { fi }, { fi } }; // [6]
		Object[] d = { fi, fi }; // [7]
		Object[][] e = { { fi }, { fi } }; // [8]

		System.out.println(fi); // [9]

		Object fi4 = true ? fi : fi; // [10]
		System.out.println(true ? fi : fi); // [11]

		int x2 = fi.foo(10); // [12]

		return fi; // [13]
	}
}

@FunctionalInterface
interface FI {
	int foo(int x);
}

@FunctionalInterface
interface F {
	FI bar(Object o);
}