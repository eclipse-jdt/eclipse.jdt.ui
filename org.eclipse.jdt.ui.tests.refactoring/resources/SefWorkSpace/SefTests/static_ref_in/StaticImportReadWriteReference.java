package static_ref_in;

import static static_in.TestStaticImportReadWrite.x;

public class StaticImportReadWriteReference {
	public void foo() {
		x= x + 10;
	}
}
