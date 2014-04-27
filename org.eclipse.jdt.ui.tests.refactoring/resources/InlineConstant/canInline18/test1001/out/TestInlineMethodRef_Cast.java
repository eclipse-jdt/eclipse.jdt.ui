// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestInlineMethodRef_Cast {
	private static int m(int x) {
		return x++;
	}

	private FI fun3() {
		F a = o -> null;
		return a.bar((FI) TestInlineMethodRef_Cast::m); // [1]
	}

	private Object fun5() {
		Object o = (FI) TestInlineMethodRef_Cast::m; // [2]
		F fx = (z) -> {
			z = (FI) TestInlineMethodRef_Cast::m; // [3]
			return null;
		};

		Object fi2;
		fi2 = (FI) TestInlineMethodRef_Cast::m; // [4]

		Object[] b = new Object[] { (FI) TestInlineMethodRef_Cast::m, (FI) TestInlineMethodRef_Cast::m }; // [5]
		Object[][] c = new Object[][] { { (FI) TestInlineMethodRef_Cast::m }, { (FI) TestInlineMethodRef_Cast::m } }; // [6]
		Object[] d = { (FI) TestInlineMethodRef_Cast::m, (FI) TestInlineMethodRef_Cast::m }; // [7]
		Object[][] e = { { (FI) TestInlineMethodRef_Cast::m }, { (FI) TestInlineMethodRef_Cast::m } }; // [8]

		System.out.println((FI) TestInlineMethodRef_Cast::m); // [9]

		Object fi4 = true ? (FI) TestInlineMethodRef_Cast::m : (FI) TestInlineMethodRef_Cast::m; // [10]
		System.out.println(true ? (FI) TestInlineMethodRef_Cast::m : (FI) TestInlineMethodRef_Cast::m); // [11]

		int x2 = ((FI) TestInlineMethodRef_Cast::m).foo(10); // [12]

		return (FI) TestInlineMethodRef_Cast::m; // [13]
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