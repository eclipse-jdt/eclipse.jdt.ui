// 5, 32 -> 5, 43  replaceAll == true, removeDeclaration == true
package p;

class LeVinSuperieure {
	public LeVinSuperieure(final String appelation) {
		String leNom= appelation == null ? "Pharmacology" : appelation;
		System.out.println("Nous avons cree un superieure vin, appelle " + leNom);
	}
}