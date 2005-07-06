//7, 36 -> 7, 36  replaceAll == true, removeDeclaration == false
package p;

@Test.Annot(Test.Annot.DEFAULT)
class Test {
	@interface Annot {
		public static final String DEFAULT= "John";
		String value();
	}
	@Annot(Annot.DEFAULT)
	int a;
	@Annot(value=Annot.DEFAULT)
	int b;
}

@Test.Annot(value=Test.Annot.DEFAULT)
enum Test2 {}