// 5, 32 -> 5, 34  replaceAllReferences == true,  removeDeclaration == true
package p;

class PartOfDeclNameSelected {
	public static final long foooo= 1;
	static {
		long l= foooo;	
	}
}