//5, 18 -> 5, 18  replaceAll == true, removeDeclaration == true
package p;

@interface Annot {
	String value();
}
@Annot("Paul")
class Test {
	@Annot(value="Paul")
	String m(Annot a) {
		return "Paul";
	}
}
