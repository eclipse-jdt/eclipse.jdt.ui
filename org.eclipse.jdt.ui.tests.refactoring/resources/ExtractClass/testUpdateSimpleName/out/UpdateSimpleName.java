package p;

public class UpdateSimpleName {
	private UpdateSimpleNameParameter parameterObject = new UpdateSimpleNameParameter();
	public void foo() {
		parameterObject.setFoo(parameterObject.getFoo2());
		this.parameterObject.setFoo(parameterObject.getFoo2());
		parameterObject.setFoo(this.parameterObject.getFoo2());
		parameterObject.setFoo(Math.abs(parameterObject.getFoo()));
	}
}
