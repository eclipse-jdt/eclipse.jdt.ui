package org.eclipse.jdt.internal.corext.codemanipulation;

public class CodeGenerationSettings {
	
	public boolean createComments= true;
	public boolean createNonJavadocComments= true;
	
	public String[] importOrder= new String[0];
	public int importThreshold= 99;
	
	public String[] fieldPrefixes;
	public String[] fieldSuffixes;
	
	public int tabWidth;

}

