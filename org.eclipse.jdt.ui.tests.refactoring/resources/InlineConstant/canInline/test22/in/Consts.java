package q;

import p.A;
import p.B;
import static p.A.getCount2;
import static p.Third.B;

public interface Consts {
	int I= 1;
	int III= I + A.getCount() + getCount2() + Third.A + B;
}