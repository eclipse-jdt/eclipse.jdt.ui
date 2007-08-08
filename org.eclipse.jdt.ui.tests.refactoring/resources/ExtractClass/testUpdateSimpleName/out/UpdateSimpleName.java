package p;

public class UpdateSimpleName {
	private UpdateSimpleNameParameter parameterObject = new UpdateSimpleNameParameter();
	public void foo() {
		parameterObject.setFoo(parameterObject.getFoo2());
		this.parameterObject.setFoo(parameterObject.getFoo2());
		parameterObject.setFoo(this.parameterObject.getFoo2());
		parameterObject.setFoo(Math.abs(parameterObject.getFoo()));
		UpdateSimpleName usn= new UpdateSimpleName();
		usn.parameterObject.setFoo(usn.parameterObject.getFoo2());
		usn.parameterObject.setFoo(parameterObject.getFoo());
		usn.parameterObject.setFoo2(usn.parameterObject.getFoo2() + 1);
	}
}
