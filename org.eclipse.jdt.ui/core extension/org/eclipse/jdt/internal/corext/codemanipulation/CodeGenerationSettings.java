package org.eclipse.jdt.internal.corext.codemanipulation;

public class CodeGenerationSettings {
	
	public boolean createFileComments= true;
	public boolean createComments= true;
	public boolean createNonJavadocComments= true;
	
	public String[] importOrder= new String[0];
	public int importThreshold= 99;
	
	/**
	 * @deprecated
	 */
	public String[] fieldPrefixes;
	/**
	 * @deprecated
	 */	
	public String[] fieldSuffixes;
	
	public int tabWidth;
	
	public void setSettings(CodeGenerationSettings settings) {
		settings.createFileComments= createFileComments;
		settings.createComments= createComments;
		settings.createNonJavadocComments= createNonJavadocComments;
		settings.importOrder= importOrder;
		settings.importThreshold= importThreshold;
		settings.fieldPrefixes= fieldPrefixes;
		settings.fieldSuffixes= fieldSuffixes;
		settings.tabWidth= tabWidth;
	}
	

}

