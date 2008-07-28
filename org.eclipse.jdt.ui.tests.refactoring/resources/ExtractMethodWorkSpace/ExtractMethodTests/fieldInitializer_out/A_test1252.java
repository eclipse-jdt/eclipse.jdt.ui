package fieldInitializer_out;

public class A_test1252 {
	String fS= "foo";
	void m() {
		new Thread() {
			String fSub= /*]*/extracted()/*[*/;
			protected String extracted() {
				return fS.substring(1);
			}
			public void run() {
				System.out.println(extracted());
			};
		}.start();
	}
}
