package import_out;

import java.io.File;

public class TestUseInArgument {
	public void main() {
		Provider p= null;
		File file = p.useAsReturn();
		file= null;
	}
}
