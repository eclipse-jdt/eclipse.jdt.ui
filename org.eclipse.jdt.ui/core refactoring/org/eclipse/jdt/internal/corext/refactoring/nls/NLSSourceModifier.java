/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.text.Region;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;

public class NLSSourceModifier {

	private final String fSubstitutionPattern;

	private NLSSourceModifier(String substitutionPattern) {
		fSubstitutionPattern= substitutionPattern;
	}

	public static Change create(ICompilationUnit cu, NLSSubstitution[] subs, String substitutionPattern, IPackageFragment accessorPackage, String accessorClassName) throws CoreException {

		NLSSourceModifier sourceModification= new NLSSourceModifier(substitutionPattern);

		String message= "Externalize strings in ''{0}''"; //$NON-NLS-1$

		TextChange change= new CompilationUnitChange(message, cu);
		MultiTextEdit multiTextEdit= new MultiTextEdit();
		change.setEdit(multiTextEdit);

		accessorClassName= sourceModification.createImportForAccessor(multiTextEdit, accessorClassName, accessorPackage, cu);

		for (int i= 0; i < subs.length; i++) {
			NLSSubstitution substitution= subs[i];
			int newState= substitution.getState();
			if (substitution.hasStateChanged()) {
				if (newState == NLSSubstitution.EXTERNALIZED) {
					if (substitution.getInitialState() == NLSSubstitution.INTERNALIZED) {
						sourceModification.addNLS(substitution, change, accessorClassName);
					} else if (substitution.getInitialState() == NLSSubstitution.IGNORED) {
						sourceModification.addAccessor(substitution, change, accessorClassName);
					}
				} else if (newState == NLSSubstitution.INTERNALIZED) {
					if (substitution.getInitialState() == NLSSubstitution.IGNORED) {
						sourceModification.deleteTag(substitution, change);
						if (substitution.isValueRename()) {
							sourceModification.replaceValue(substitution, change);
						}
					} else if (substitution.getInitialState() == NLSSubstitution.EXTERNALIZED) {
						sourceModification.deleteAccessor(substitution, change);
						sourceModification.deleteTag(substitution, change);
					}
				} else if (newState == NLSSubstitution.IGNORED) {
					if (substitution.getInitialState() == NLSSubstitution.INTERNALIZED) {
						sourceModification.addNLS(substitution, change, accessorClassName);
						if (substitution.isValueRename()) {
							sourceModification.replaceValue(substitution, change);
						}
					} else {
						if (substitution.getInitialState() == NLSSubstitution.EXTERNALIZED) {
							sourceModification.deleteAccessor(substitution, change);
						}
					}
					}
			} else {
				if (newState == NLSSubstitution.EXTERNALIZED) {
					if (substitution.isKeyRename()) {
						sourceModification.replaceKey(substitution, change);
					}
					if (substitution.isAccessorRename()) {
						sourceModification.replaceAccessor(substitution, change);
					}
				} else {
					if (substitution.isValueRename()) {
						sourceModification.replaceValue(substitution, change);
					}
				}
			}
		}

		return change;
	}

	/**
	 * @param substitution
	 * @param change
	 */
	private void replaceAccessor(NLSSubstitution substitution, TextChange change) {
		AccessorClassInfo accessorClassInfo= substitution.getAccessorClassInfo();
		if (accessorClassInfo != null) {
			Region region= accessorClassInfo.getRegion();
			int len= accessorClassInfo.getName().length();
			TextChangeCompatibility.addTextEdit(change, "", //$NON-NLS-1$
					new ReplaceEdit(region.getOffset(), len, substitution.getUpdatedAccessor())); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
	}

	private void replaceKey(NLSSubstitution substitution, TextChange change) {
		Region region= substitution.getNLSElement().getPosition();
		TextChangeCompatibility.addTextEdit(change, "", //$NON-NLS-1$
				new ReplaceEdit(region.getOffset(), region.getLength(), "\"" + unwindEscapeChars(substitution.getKey()) + "\"")); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	private void replaceValue(NLSSubstitution substitution, TextChange change) {
		Region region= substitution.getNLSElement().getPosition();
		TextChangeCompatibility.addTextEdit(change, "", //$NON-NLS-1$
				new ReplaceEdit(region.getOffset(), region.getLength(), "\"" + unwindEscapeChars(substitution.getValue()) + "\"")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void deleteAccessor(NLSSubstitution substitution, TextChange change) {
		AccessorClassInfo accessorClassInfo= substitution.getAccessorClassInfo();
		if (accessorClassInfo != null) {
			Region region= accessorClassInfo.getRegion();
			TextChangeCompatibility.addTextEdit(change, "", new ReplaceEdit(region.getOffset(), region.getLength(), "\"" + unwindEscapeChars(substitution.getValue()) + "\"")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	// TODO: not dry
	private String unwindEscapeChars(String s) {
		if (s != null) {
			StringBuffer sb= new StringBuffer(s.length());
			int length= s.length();
			for (int i= 0; i < length; i++) {
				char c= s.charAt(i);
				sb.append(getUnwoundString(c));
			}
			return sb.toString();
		}
		return null;
	}

	private String getUnwoundString(char c) {
		switch (c) {
			case '\b' :
				return "\\b";//$NON-NLS-1$
			case '\t' :
				return "\\t";//$NON-NLS-1$
			case '\n' :
				return "\\n";//$NON-NLS-1$
			case '\f' :
				return "\\f";//$NON-NLS-1$	
			case '\r' :
				return "\\r";//$NON-NLS-1$
			case '\\' :
				return "\\\\";//$NON-NLS-1$
		}
		return String.valueOf(c);
	}

	private void deleteTag(NLSSubstitution substitution, TextChange change) {
		Region textRegion= substitution.getNLSElement().getTagPosition();

		TextChangeCompatibility.addTextEdit(change, "delete Tag", //$NON-NLS-1$
				new DeleteEdit(textRegion.getOffset(), textRegion.getLength()));
	}

	private String createImportForAccessor(MultiTextEdit parent, String accessorClassName, IPackageFragment accessorPackage, ICompilationUnit cu) throws CoreException {

		ITextFileBufferManager manager= FileBuffers.getTextFileBufferManager();
		IPath path= cu.getPath();
		try {
			manager.connect(path, null);
			
			IType type= accessorPackage.getCompilationUnit(accessorClassName + ".java").getType(accessorClassName); //$NON-NLS-1$
			String fullyQualifiedName= type.getFullyQualifiedName();

			ImportRewrite importRewrite= new ImportRewrite(cu);
			String nameToUse= importRewrite.addImport(fullyQualifiedName);
			TextEdit edit= importRewrite.createEdit(manager.getTextFileBuffer(path).getDocument());
			parent.addChild(edit);

			return nameToUse;
		} finally {
			manager.disconnect(path, null);
		}
	}

	private void addNLS(NLSSubstitution sub, TextChange change, String accessorName) {
		if (sub.getState() == NLSSubstitution.INTERNALIZED)
			return;

		String text= addAccessor(sub, change, accessorName);

		NLSElement element= sub.getNLSElement();
		String[] args= {text, element.getValue()};
		String name= NLSMessages.getFormattedString("NLSrefactoring.add_tag", args); //$NON-NLS-1$
		TextChangeCompatibility.addTextEdit(change, name, createAddTagChange(element));
	}

	private String addAccessor(NLSSubstitution sub, TextChange change, String accessorName) {
		Region position= sub.getNLSElement().getPosition();
		String text= "Externalize string: {0}"; //$NON-NLS-1$

		if (sub.getState() == NLSSubstitution.EXTERNALIZED) {
			String resourceGetter= createResourceGetter(sub.getKey(), accessorName);
			TextChangeCompatibility.addTextEdit(change, text, new ReplaceEdit(position.getOffset(), position.getLength(), resourceGetter));
		}
		return text;
	}

	private TextEdit createAddTagChange(NLSElement element) {
		int offset= element.getTagPosition().getOffset(); //to be changed
		String text= " " + element.getTagText(); //$NON-NLS-1$
		return new InsertEdit(offset, text);
	}

	private String createResourceGetter(String key, String accessorName) {
		StringBuffer buf= new StringBuffer();
		buf.append(accessorName);
		buf.append('.');

		//we just replace the first occurrence of KEY in the pattern
		int i= fSubstitutionPattern.indexOf(NLSRefactoring.KEY);
		if (i != -1) {
			buf.append(fSubstitutionPattern.substring(0, i));
			buf.append('"').append(key).append('"');
			buf.append(fSubstitutionPattern.substring(i + NLSRefactoring.KEY.length()));
		}
		return buf.toString();
	}
}