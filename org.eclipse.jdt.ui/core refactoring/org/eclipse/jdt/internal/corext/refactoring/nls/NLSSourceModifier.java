/*****************************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

public class NLSSourceModifier {

	private final String fSubstitutionPattern;
	private final String fDefaultSubstitution;

	private NLSSourceModifier(String defaultSubstitution,
		String substitutionPattern, String substitutionPrefix) {		
		fSubstitutionPattern= substitutionPattern;
		fDefaultSubstitution= defaultSubstitution;					
	}

	public static Change create(
			ICompilationUnit cu, 
			NLSSubstitution[] subs, 
			String defaultSubstitution,
			String substitutionPattern, 
			String substitutionPrefix, 
			boolean willCreateAccessor, 
			IPackageFragment accessorPackage,
			String accessorClassName) throws CoreException {

		NLSSourceModifier sourceModification= 
		    new NLSSourceModifier(defaultSubstitution, substitutionPattern, substitutionPrefix);

		String message= NLSMessages.getFormattedString("NLSRefactoring.externalize_strings", //$NON-NLS-1$
			cu.getElementName());

		TextChange change= new CompilationUnitChange(message, cu);
		MultiTextEdit multiTextEdit= new MultiTextEdit();
		change.setEdit(multiTextEdit);

		if (willCreateAccessor) {
			accessorClassName= sourceModification.createImportForAccessor(multiTextEdit, accessorClassName, accessorPackage, cu);
		}
		
		for (int i = 0; i < subs.length; i++) {
            NLSSubstitution substitution = subs[i];
            if (substitution.hasChanged()) {
                if (substitution.getState() == NLSSubstitution.EXTERNALIZED) {
                    if (substitution.getOldState() == NLSSubstitution.INTERNALIZED) { 
                        sourceModification.addNLS(substitution, change, accessorClassName, substitutionPrefix);
                    } else if (substitution.getOldState() == NLSSubstitution.IGNORED) {
                        sourceModification.addAccessor(substitution, change, accessorClassName, substitutionPrefix);                  
                    }
                } else if (substitution.getState() == NLSSubstitution.INTERNALIZED) {
                    if (substitution.getOldState() == NLSSubstitution.IGNORED) {
                        sourceModification.deleteTag(substitution, change);
                    } else if (substitution.getOldState() == NLSSubstitution.EXTERNALIZED) {
                        sourceModification.deleteAccessor(substitution, change);
                        sourceModification.deleteTag(substitution, change);
                    }
                } else if (substitution.getState() == NLSSubstitution.IGNORED) {
                    if (substitution.getOldState() == NLSSubstitution.INTERNALIZED) {
                        sourceModification.addNLS(substitution, change, accessorClassName, substitutionPrefix);                        
                    } else if (substitution.getOldState() == NLSSubstitution.EXTERNALIZED) {
                        sourceModification.deleteAccessor(substitution, change);
                    }                    
                }
            }
        }   

		return change;
	}

    private void deleteAccessor(NLSSubstitution substitution, TextChange change) {
        AccessorClassInfo accessorClassInfo = substitution.getAccessorClassInfo();
        if (substitution.getAccessorClassInfo() != null) {
            TextChangeCompatibility.addTextEdit(
                    change, 
                    "", 
                    new ReplaceEdit(accessorClassInfo.fOffset, 
                            accessorClassInfo.fLength, 
                            "\"" + substitution.fValue + "\""));
        }           
    }

    private void deleteTag(NLSSubstitution substitution, TextChange change) {
        TextRegion textRegion = substitution.fNLSElement.getTagPosition();
        
        TextChangeCompatibility.addTextEdit(
                change, 
                "delete Tag", 
                new DeleteEdit(textRegion.getOffset(), textRegion.getLength()));        
    }

    private String createImportForAccessor(MultiTextEdit parent,
            String accessorClassName, 
            IPackageFragment accessorPackage,
            ICompilationUnit cu) throws CoreException {

		TextBuffer buffer= null;
		try {
			IType type= accessorPackage.getCompilationUnit(accessorClassName + ".java").getType(accessorClassName); //$NON-NLS-1$
			String fullyQualifiedName= type.getFullyQualifiedName();

			ImportRewrite importRewrite= new ImportRewrite(cu);
			String nameToUse= importRewrite.addImport(fullyQualifiedName);
			buffer= TextBuffer.acquire(ResourceUtil.getFile(cu));
			TextEdit edit= importRewrite.createEdit(buffer.getDocument());
			parent.addChild(edit);

			return nameToUse;
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
		}

	}

	private void addNLS(NLSSubstitution sub, TextChange change, String accessorName, String substitutionPrefix) {
        if (sub.fState == NLSSubstitution.INTERNALIZED) return;
        
        String text = addAccessor(sub, change, accessorName, substitutionPrefix);
        
        NLSElement element = sub.fNLSElement;
        String[] args = { text, element.getValue()};
        String name = NLSMessages.getFormattedString("NLSrefactoring.add_tag", args); //$NON-NLS-1$
        TextChangeCompatibility.addTextEdit(change, name, createAddTagChange(element));
    }
    
    private String addAccessor(NLSSubstitution sub, TextChange change, String accessorName, String substitutionPrefix) {
        TextRegion position = sub.fNLSElement.getPosition();
        String text = NLSMessages.getFormattedString("NLSrefactoring.extrenalize_string", sub.fNLSElement.getValue()); //$NON-NLS-1$
        
        if (sub.fState == NLSSubstitution.EXTERNALIZED) {
            String resourceGetter = createResourceGetter(sub.getKeyWithPrefix(substitutionPrefix), accessorName);
            TextChangeCompatibility.addTextEdit(change, text, new ReplaceEdit(position.getOffset(), 
                    position.getLength(), 
                    resourceGetter));
        }
        return text;
    }

	private TextEdit createAddTagChange(NLSElement element) {
		int offset= element.getTagPosition().getOffset(); //to be changed
		String text= " " + element.getTagText();
		return new InsertEdit(offset, text);
	}

	private String createResourceGetter(String key, String accessorName) {
		StringBuffer buff= null;
		if (fSubstitutionPattern.equals(fDefaultSubstitution) == true) {
			buff= new StringBuffer(NLSRefactoring.getDefaultSubstitutionPattern(accessorName));
		} else {
			buff= new StringBuffer(fSubstitutionPattern);
		}

		//we just replace the first occurrence of KEY in the pattern
		int i= buff.indexOf(NLSRefactoring.KEY);
		if (i != -1)
			buff.replace(i, i + NLSRefactoring.KEY.length(), '"' + key + '"');
		return buff.toString();
	}	
}
