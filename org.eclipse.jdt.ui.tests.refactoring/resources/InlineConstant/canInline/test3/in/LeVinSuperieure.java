// 5, 32 -> 5, 43  replaceAll == true, removeDeclaration == true
package p;

class LeVinSuperieure {
	public static final String LE_CONSTANT= "Pharmacology";
	
	public LeVinSuperieure(final String appelation) {
		String leNom= appelation == null ? LE_CONSTANT : appelation;
		System.out.println("Nous avons cree un superieure vin, appelle " + leNom);
	}
}