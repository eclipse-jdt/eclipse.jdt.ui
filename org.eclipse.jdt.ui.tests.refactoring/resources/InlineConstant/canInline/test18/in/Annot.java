//5, 18 -> 5, 18  replaceAll == true, removeDeclaration == true
package p;

@interface Annot {
	String DEFAULT_NAME= "Paul";
	String value();
}
@Annot(Annot.DEFAULT_NAME)
class Test {
	@Annot(value=Annot.DEFAULT_NAME)
	String m(Annot a) {
		return a.DEFAULT_NAME;
	}
}
