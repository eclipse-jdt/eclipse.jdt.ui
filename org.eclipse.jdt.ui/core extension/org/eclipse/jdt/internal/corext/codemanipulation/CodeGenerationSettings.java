package org.eclipse.jdt.internal.corext.codemanipulation;

public class CodeGenerationSettings {
	
	/**
	 * @deprecated
	 */
	public boolean createFileComments= true;

	public boolean createComments= true;
	
	/**
	 * @deprecated
	 */
	public boolean createNonJavadocComments= true;
	
	public String[] importOrder= new String[0];
	public int importThreshold= 99;
		
	public int tabWidth;
	
	public void setSettings(CodeGenerationSettings settings) {
		settings.createComments= createComments;
		settings.importOrder= importOrder;
		settings.importThreshold= importThreshold;
		settings.tabWidth= tabWidth;
	}
	

}

