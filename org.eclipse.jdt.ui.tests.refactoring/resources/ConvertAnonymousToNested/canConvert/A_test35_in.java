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
		this(new ISup() {
			private final long x= d.getTime();

			public void m() {
				System.out.println(x);
			}
		});
	}

	public Sub(ISup p) {
		super(p);
	}
}