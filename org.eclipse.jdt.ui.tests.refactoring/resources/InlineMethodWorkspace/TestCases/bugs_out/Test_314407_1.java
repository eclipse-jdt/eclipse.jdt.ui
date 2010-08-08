package bugs_out;

public class Test_314407_1 {
	void foo() {
		Object x=getImage();
		Object y=null;
		Object z=getImage(x);
	}
}
