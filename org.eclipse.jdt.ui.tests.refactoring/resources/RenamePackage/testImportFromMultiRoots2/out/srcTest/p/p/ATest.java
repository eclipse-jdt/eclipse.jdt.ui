package p.p;

import q.A;
import q.*; //myself

public class ATest {
	A aFromOtherPackageFragment;
	q.A aQualifiedFromNamesake;
	
	public void test1() {
		TestHelper.log("x");
	}
}
