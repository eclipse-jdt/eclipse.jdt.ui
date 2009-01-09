// 7, 35 -> 7, 35  replaceAll == true, removeDeclaration == false;
package cantonzuerich;

public class GrueziWohl {
	private static String gruezi= "Gruezi";
	private static boolean jh= true;
	private static final boolean WOHL= jh && "Gruezi".equals(gruezi);
	
	public String holenGruss() {
		String gruezi= "Gruezi";
		return gruezi + (jh && "Gruezi".equals(GrueziWohl.gruezi) ? " Wohl" : "") + "!";
	}
	
	private boolean wohl() {
		return jh && "Gruezi".equals(gruezi);
	}
}