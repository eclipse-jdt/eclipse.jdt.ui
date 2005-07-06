//7, 36 -> 7, 36  replaceAll == true, removeDeclaration == false
package p;

@Test.Annot("John")
class Test {
	@interface Annot {
		public static final String DEFAULT= "John";
		String value();
	}
	@Annot("John")
	int a;
	@Annot(value="John")
	int b;
}

@Test.Annot(value="John")
enum Test2 {}