package a.b.c;

import p.ATest.ATestI;
import p.ATest.ATestI.ATestIIb;
import a.b.c.A.ARunner;

public class B {
	public class BRunner implements Runnable {
		public void run() {
			(new ATestI()).new ATestII();
			new ATestIIb();
		}
	}
	ARunner ar;
}
