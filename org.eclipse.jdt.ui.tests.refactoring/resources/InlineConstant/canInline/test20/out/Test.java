//10, 21 -> 10, 21  replaceAll == true, removeDeclaration == true
package p;

class Test {
	private enum Color {
		PINK, YELLOW
	}
	private enum Box {
		FIRST(Test.Color.PINK);
		public Box(Color c) {}
	}
	Color c= Test.Color.PINK;
}
