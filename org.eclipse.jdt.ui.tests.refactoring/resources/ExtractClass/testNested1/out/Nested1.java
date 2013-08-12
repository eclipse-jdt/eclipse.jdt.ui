package p;

class Nested1 {
    class Inner {
        public class Extracted {
			public int m;
			public Extracted() {
			}
		}

		Extracted parameterObject = new Extracted();
    }
}