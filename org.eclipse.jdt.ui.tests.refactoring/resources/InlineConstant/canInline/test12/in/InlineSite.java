// Here, an import is added for a type needed only after a qualification is added.
// 7, 37 -> 7, 43  replaceAll == true, removeDeclaration == false
package p2;

class InlineSite {
	static {
		System.out.println(p1.Declarer.CONSTANT);	
	}
}