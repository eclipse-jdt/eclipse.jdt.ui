// Here, an import is added for a type needed only after a qualification is added.
// 7, 37 -> 7, 43  replaceAll == true, removeDeclaration == false
package p2;

import p1.Declarer;

class InlineSite {
	static {
		System.out.println(Declarer.foo);	
	}
}