package import_out;

import java.io.File;
import java.util.Map;

public class TestUseInLocalClass {
	public void main() {
		Provider p= null;
		class Local extends File implements Comparable {
			public Local(String s) {
				super(s);
			}
			public void foo(Map map) {
			}
			public int compareTo(Object o) {
				return 0;
			}
		}
	}
}
