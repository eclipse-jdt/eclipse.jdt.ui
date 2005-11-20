package static_ref_out;

import static static_out.TestStaticImportRead.getX;

public class StaticImportReadReference {
	public void foo() {
		int y= getX();
	}
}
