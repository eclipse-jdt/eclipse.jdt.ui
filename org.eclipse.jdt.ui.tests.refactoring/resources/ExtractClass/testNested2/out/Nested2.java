package p;

interface Nested2 {
    class Inner {
        public static class Extracted {
			public int m;
			public Extracted() {
			}
		}

		Extracted parameterObject = new Extracted();
    }
}