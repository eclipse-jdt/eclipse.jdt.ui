package org.eclipse.jdt.internal.ui.text.correction;

public class SimilarElement {
	
	public int fKind;
	public String fName;
	public int fRelevance;

	public SimilarElement(int kind, String name, int relevance) {
		fKind= kind;
		fName= name;
		fRelevance= relevance;
	}

	/**
	 * Gets the kind.
	 * @return Returns a int
	 */
	public int getKind() {
		return fKind;
	}

	/**
	 * Gets the name.
	 * @return Returns a String
	 */
	public String getName() {
		return fName;
	}

	/**
	 * Gets the relevance.
	 * @return Returns a int
	 */
	public int getRelevance() {
		return fRelevance;
	}
	
	/* (non-Javadoc)
	 * @see Object#equals(Object)
	 */
	public boolean equals(Object obj) {
		if (obj instanceof SimilarElement) {
			SimilarElement elem= (SimilarElement) obj;
			return fName.equals(elem.fName) && fKind == elem.fKind;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see Object#hashCode()
	 */
	public int hashCode() {
		return fName.hashCode() + fKind;
	}	
}