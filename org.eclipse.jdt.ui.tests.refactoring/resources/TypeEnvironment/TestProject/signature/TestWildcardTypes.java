package signature;

import java.util.List;

public class TestWildcardTypes {
	void foo() {
		List<? extends String> le;
		List<? super String> ls;
		List<?> l;
	}
}
