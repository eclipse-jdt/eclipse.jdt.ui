package q;

import p.A;
import r.Third;
import static p.A.getCount2;
import static r.Third.B;

public interface Consts {
	int I= 1;
	int III= I + A.getCount() + getCount2() + Third.A + B;
}