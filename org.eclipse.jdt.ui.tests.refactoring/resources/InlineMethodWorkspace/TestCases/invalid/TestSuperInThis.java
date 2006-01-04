package invalid;

public class TestSuperInThis {
	public void main() {
		new TestSuperInThis(){
			public void f(){
				int u= /*[*/TestSuperInThis.this.toInline()/*]*/;
			}
		};
	}
	
	public int toInline() {
		return super.hashCode();
	}
}
