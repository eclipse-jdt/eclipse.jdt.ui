package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import java.io.IOException;import java.io.Reader;import org.eclipse.swt.graphics.GC;import org.eclipse.swt.graphics.Rectangle;import org.eclipse.swt.widgets.Display;import org.eclipse.jdt.core.IField;import org.eclipse.jdt.core.IJavaProject;import org.eclipse.jdt.core.IMember;import org.eclipse.jdt.core.IType;import org.eclipse.jdt.core.JavaModelException;import org.eclipse.jdt.core.Signature;import org.eclipse.jdt.internal.ui.JavaPlugin;import org.eclipse.jdt.internal.ui.codemanipulation.StubUtility;import org.eclipse.jdt.internal.ui.text.LineBreakingReader;import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocAccess;import org.eclipse.jdt.internal.ui.text.javadoc.JavaDocTextReader;import org.eclipse.jdt.internal.ui.util.JavaModelUtility;


public class ProposalInfo {
	
	private static final int NUMBER_OF_JAVADOC_LINES= 12;
	
	private IJavaProject fJavaProject;
	private char[] fPackageName;
	private char[] fTypeName;
	private char[] fMemberName;
	private char[][] fParameterPackages;
	private char[][] fParameterTypes;

	public ProposalInfo(IJavaProject jproject, char[] packName, char[] typeQualifiedName, char[] methodName, char[][] paramPackages, char[][] paramTypes) {
		fJavaProject= jproject;
		fPackageName= packName;
		fTypeName= typeQualifiedName;
		fMemberName= methodName;
		fParameterPackages= paramPackages;
		fParameterTypes= paramTypes;
	}
	
	public ProposalInfo(IJavaProject jproject, char[] packName, char[] typeQualifiedName) {
		this(jproject, packName, typeQualifiedName, null, null, null);
	}

	public ProposalInfo(IJavaProject jproject, char[] packName, char[] typeQualifiedName, char[] fieldName) {
		this(jproject, packName, typeQualifiedName, fieldName, null, null);
	}	
	
	/**
	 * Gets the text for this proposal info
	 */	
	public String getInfo() {
		try {
			IType type= JavaModelUtility.findType(fJavaProject, new String(fPackageName), new String(fTypeName));
			if (type != null) {
				IMember member= null;
				if (fMemberName != null) {
					String name= new String(fMemberName);
					if (fParameterTypes != null) {
						String[] paramTypes= new String[fParameterTypes.length];
						for (int i= 0; i < fParameterTypes.length; i++) {
							paramTypes[i]= getParameterSignature(i);
						}
						member= StubUtility.findMethod(name, paramTypes, false, type);
					} else {
						IField field= type.getField(name);
						if (field.exists()) {
							member= field;
						}
					}
				} else {
					member= type;
				}
				if (member != null) {
					String lineDelim= System.getProperty("line.separator", "\n");
					return getJavaDocText(member, lineDelim);
				}
			}
		} catch (JavaModelException e) {
			JavaPlugin.getDefault().log(e.getStatus());
		}
		return null;
	}
	
	private String getParameterSignature(int index) {
		StringBuffer buf= new StringBuffer();
		char[] pack= fParameterPackages[index];
		if (pack != null && pack.length > 0) {
			buf.append(pack);
			buf.append('.');
		}
		buf.append(fParameterTypes[index]);
		return Signature.createTypeSignature(buf.toString(), true);
	}
	
	private String getJavaDocText(IMember member, String lineDelim) throws JavaModelException {
		Reader rd= JavaDocAccess.getJavaDoc(member);
		if (rd != null) {
			JavaDocTextReader textReader= new JavaDocTextReader(rd);
			
			Display display= Display.getDefault();
			GC gc= new GC(display);
			try {
				StringBuffer buf= new StringBuffer();
				int maxNumberOfLines= NUMBER_OF_JAVADOC_LINES;
				
				LineBreakingReader reader= new LineBreakingReader(textReader, gc, getHoverWidth(display));
				String line= reader.readLine();
				while (maxNumberOfLines > 0 && line != null) {
					if (buf.length() != 0) {
						buf.append(lineDelim);
					}
					buf.append(' '); // add one space indent
					buf.append(line);
					line= reader.readLine();
					maxNumberOfLines--;
				}
				if (line != null) {
					buf.append(lineDelim);
					buf.append(" ...");
				}
				return buf.toString();
			} catch (IOException e) {
				JavaPlugin.log(e);
			} finally {
				gc.dispose();
			}
		}
		return null;
	}
	
	private int getHoverWidth(Display display) {
		Rectangle displayBounds= display.getBounds();
		int hoverWidth= displayBounds.width - (display.getCursorLocation().x - displayBounds.x);
		hoverWidth-= 5; // add some space to the border
		if (hoverWidth < 200) {
			hoverWidth= 200;
		}
		return hoverWidth;
	}	
		
		
	
	

}