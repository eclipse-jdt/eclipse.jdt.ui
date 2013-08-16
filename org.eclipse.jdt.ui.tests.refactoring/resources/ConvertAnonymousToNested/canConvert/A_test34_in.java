package p;

import java.util.Date;

public class Sup {
	interface ISup {
		void m();
	}

	public Sup(ISup isup) {
	}
}

class Sub extends Sup {
	public Sub(final Date d) {
		super(new ISup() {
			private final long x= d.getTime();

			public void m() {
				System.out.println(x);
			}
		});
	}
}
