package q;

import p.A;
import static p.A.getCount2;

public interface Consts {
	int I= 1;
	int III= I + Consts.I + q.Consts.I
			+ p.A.getCount() + A.getCount() + getCount2();
}