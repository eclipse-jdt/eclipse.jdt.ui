package r;

import p.A; //(-import)
import p.A.Inner; //-import (invalid)

public class B {
	Inner iFromB; //->Inner
	A.Inner iiFromB; //->Inner
	p.A.Inner iiiFromB; //->Inner
}
