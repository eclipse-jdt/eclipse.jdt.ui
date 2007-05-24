
public class TestDefaultPackagePoint {
	public static void main(String[] args) {
		new TestDefaultPackagePoint().foo(new ArrayList(5, 6));
	}
	public static class ArrayList {
		public int xer;
		public int yer;
		public ArrayList(int xer, int yer) {
			this.xer = xer;
			this.yer = yer;
		}
	}
	public void foo(ArrayList parameterObject){
		System.out.println(parameterObject.xer+parameterObject.yer);
	}
}
