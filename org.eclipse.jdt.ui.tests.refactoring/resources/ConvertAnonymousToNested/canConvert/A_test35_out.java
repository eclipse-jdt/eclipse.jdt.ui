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
	private static class Nested implements ISup {
		private final Date d;
		private final long x;
		private Nested(Date d) {
			this.d= d;
			x= d.getTime();
		}
		public void m() {
			System.out.println(x);
		}
	}

	public Sub(final Date d) {
		this(new Nested(d));
	}

	public Sub(ISup p) {
		super(p);
	}
}