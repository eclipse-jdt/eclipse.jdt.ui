//14, 17, 14, 17  replaceAll == true, removeDeclaration == true
package p;

import static p.A.getCount2;
import static q.Consts.I;
import q.Consts;

public class A {
	public static final int getCount() { return 42; }
	public static final int getCount2() { return 42; }
	
	int getIII() {
		int i= Consts.I + Consts.I + q.Consts.I
				+ p.A.getCount() + A.getCount() + getCount2();
		int ii= Consts.I + Consts.I + q.Consts.I
				+ p.A.getCount() + A.getCount() + getCount2();
		return (I + Consts.I + q.Consts.I
				+ p.A.getCount() + A.getCount() + getCount2()) + i + ii;
	}
}
