package trycatch_out;

import java.io.IOException;
import java.sql.SQLException;

public class TestMethodThrowsException {
	private void thrower() throws SQLException, IOException {
	}
	public void test() throws SQLException {
		try {
			/*[*/thrower();/*]*/
		} catch (IOException e) {
		}
	}
}
