package static_ref_out;

import static static_out.TestStaticImportReadWrite.getX;
import static static_out.TestStaticImportReadWrite.setX;

public class StaticImportReadWriteReference {
	public void foo() {
		setX(getX() + 10);
	}
}
