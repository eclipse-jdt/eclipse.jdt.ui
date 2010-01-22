package p;

public class DuplicateParamNameParameter {
	private String fHTest;
	private String fGTest;
	public DuplicateParamNameParameter(String hTest, String gTest) {
		fHTest = hTest;
		fGTest = gTest;
	}
	public String getHTest() {
		return fHTest;
	}
	public void setHTest(String hTest) {
		fHTest = hTest;
	}
	public String getGTest() {
		return fGTest;
	}
	public void setGTest(String gTest) {
		fGTest = gTest;
	}
}