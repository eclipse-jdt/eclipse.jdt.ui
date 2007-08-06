package p;

public class DuplicateParamNameParameter {
	private String fHTest;
	private String fGTest;
	public DuplicateParamNameParameter(String test, String test2) {
		fHTest = test;
		fGTest = test2;
	}
	public String getHTest() {
		return fHTest;
	}
	public void setHTest(String test) {
		fHTest = test;
	}
	public String getGTest() {
		return fGTest;
	}
	public void setGTest(String test) {
		fGTest = test;
	}
}