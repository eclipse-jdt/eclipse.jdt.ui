// Here, an import is added for a type needed only after a qualification is added.
// 8, 19 -> 8, 29  removeAll == false
package p2;

import p1.A.B;

class InlineSite {
	Object thing= B.CONSTANT;	
}