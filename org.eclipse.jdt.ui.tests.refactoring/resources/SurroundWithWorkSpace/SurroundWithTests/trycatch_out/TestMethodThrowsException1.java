package trycatch_out;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class TestMethodThrowsException1 {
	private void thrower() throws SQLException, FileNotFoundException {
	}
	public void test() throws IOException {
		try {
			/*[*/thrower();/*]*/
		} catch (SQLException e) {
		}
	}
}
