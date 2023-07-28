public class Foo {

	int topInt= 0;

	static int topStaticInt= 0;

	public void topInstance() {
		topInt++;
		Foo.this.topInt= this.topInt;

		method();
		this.method();
		Foo.this.method();
	}

	public void method() {
	}

	public static void topStatic() {
		topStaticInt++;
	}

	class Nested1 {
		int nested1Int= 0;

		static int nested1StaticInt= 0;

		public void nested1Instance() {
			topInt++;
			Foo.this.topInt= topInt;
			nested1Int++;
			this.nested1Int++;
			Nested1.this.nested1Int++;

			topInstance();
			Foo.this.topInstance();
			topStatic();
		}

		public static void nested1Static() {
			nested1StaticInt++;
		}

		class Nested2 {
			int nested2Int= 0;

			static int nested2StaticInt= 0;

			public void nested2Instance() {
				topInt++;
				Foo.this.topInt= topInt;
				nested1Int++;
				Nested1.this.nested1Int++;
				nested2Int++;
				this.nested2Int++;
				Nested2.this.nested2Int++;

				topInstance();
				Foo.this.topInstance();
				topStatic();
				nested1Instance();
				Nested1.this.nested1Instance();
				Nested1.nested1Static();
				nested2Instance();
				this.nested2Instance();
				Nested2.nested2Static();
			}

			public static void nested2Static() {
				nested2StaticInt++;
			}
		}
	}

	public void testAnonymousClass() {
		new Object() {
			int anonymousInt= 0;

			public void anonymousMethod() {
				anonymousInt++;
				topInt++;
				topStatic();
				topInstance();
				new Nested1().nested1Instance();
				Nested1.Nested2.nested2Static();
			}
		};
	}
}
