package import_out;

import java.io.File;
import java.util.Map;

public class TestUseInLocalClass {
	public void main() {
		Provider p= null;
		class Local extends File {
			private static final long serialVersionUID = 1L;
			public Local(String s) {
				super(s);
			}
			public void foo(Map map) {
			}
			public void bar(Byte b) {
			}
		}
	}
}
