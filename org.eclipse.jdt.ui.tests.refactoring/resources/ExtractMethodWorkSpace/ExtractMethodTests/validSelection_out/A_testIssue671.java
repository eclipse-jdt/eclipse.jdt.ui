package validSelection_out;

public class A_testIssue671 {
	protected void foo() {
		while (true) {
			List<String> line = List.of();
			if (line == null) {
				break;
			}
			MyType plcTableType = extracted();

			if (plcTableType == null) {
				return;
			}
		}
	}

	protected MyType extracted() {
		/*[*/// comment
		MyType plcTableType;
		String plcTableTypeText = "";
		if (plcTableTypeText.isEmpty()) {
			plcTableType = null;
		} else {
			plcTableType = new MyType();
		}
		// comment 2/*]*/
		return plcTableType;
	}

	public static class MyType {
	}
}
