package p;

public class A {
		static final String EXT2_CLASS = ".class";

		enum Replacer2 {
			NILL1(EXT2_CLASS),
			NILL2(A.EXT2_CLASS); 

			String s;

			Replacer2(String s) {
				this.s = s;
			}
		}
	}