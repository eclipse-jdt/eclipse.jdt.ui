// 14, 16 -> 14, 32  replaceAll == true, removeDeclaration == false
package schweiz.zuerich.zuerich;

public abstract class Froehlichkeit {
	static class MeineFroehlichkeit extends Froehlichkeit {
		MeineFroehlichkeit(Object o) {}
	}
	private static Object something= new Object();
	private static final Froehlichkeit dieFroehlichkeit= new MeineFroehlichkeit(something);

	public Froehlichkeit holenFroehlichkeit() {
		class MeineFroehlichkeit {
		}
		return new Froehlichkeit.MeineFroehlichkeit(something);
	}

	public Froehlichkeit deineFroehlichkeit() {
		Object something= "";
		return new MeineFroehlichkeit(Froehlichkeit.something);
	}
}