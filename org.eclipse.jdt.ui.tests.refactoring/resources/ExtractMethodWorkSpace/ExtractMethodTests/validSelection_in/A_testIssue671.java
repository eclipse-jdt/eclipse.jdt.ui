package validSelection_in;

public class A_testIssue671 {
	protected void foo() {
		while (true) {
			List<String> line = List.of();
			if (line == null) {
				break;
			}
			/*[*/// comment
			MyType plcTableType;
			String plcTableTypeText = "";
			if (plcTableTypeText.isEmpty()) {
				plcTableType = null;
			} else {
				plcTableType = new MyType();
			}
			// comment 2/*]*/

			if (plcTableType == null) {
				return;
			}
		}
	}

	public static class MyType {
	}
}
