// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class TestInlineLambda_Cast {
	private FI fun3() {
		F a = o -> null;
		return a.bar((FI) x -> x++); // [1]
	}

	private Object fun5() {
		Object o = (FI) x -> x++; // [2]
		F fx = (z) -> {
			z = (FI) x -> x++; // [3]
			return null;
		};

		Object fi2;
		fi2 = (FI) x -> x++; // [4]

		Object[] b = new Object[] { (FI) x -> x++, (FI) x -> x++ }; // [5]
		Object[][] c = new Object[][] { { (FI) x -> x++ }, { (FI) x -> x++ } }; // [6]
		Object[] d = { (FI) x -> x++, (FI) x -> x++ }; // [7]
		Object[][] e = { { (FI) x -> x++ }, { (FI) x -> x++ } }; // [8]

		System.out.println((FI) x -> x++); // [9]

		Object fi4 = true ? (FI) x -> x++ : (FI) x -> x++; // [10]
		System.out.println(true ? (FI) x -> x++ : (FI) x -> x++); // [11]

		int x2 = ((FI) x -> x++).foo(10); // [12]

		return (FI) x -> x++; // [13]
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