package trycatch_in;

import java.io.IOException;
import java.sql.SQLException;

public class TestMethodThrowsException {
	private void thrower() throws SQLException, IOException {
	}
	public void test() throws SQLException {
		/*[*/thrower();/*]*/
	}
}
