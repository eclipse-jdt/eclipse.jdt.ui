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
		String substitutionPattern) {		
		fSubstitutionPattern= substitutionPattern;
		fDefaultSubstitution= defaultSubstitution;					
	}

	public static Change create(
			ICompilationUnit cu, 
			NLSSubstitution[] subs, 
			String defaultSubstitution,
			String substitutionPattern,
			boolean mustCreateImport, 
			IPackageFragment accessorPackage,
			String accessorClassName) throws CoreException {

		NLSSourceModifier sourceModification= 
		    new NLSSourceModifier(defaultSubstitution, substitutionPattern);

		String message= NLSMessages.getFormattedString("NLSRefactoring.externalize_strings", //$NON-NLS-1$
			cu.getElementName());

		TextChange change= new CompilationUnitChange(message, cu);
		MultiTextEdit multiTextEdit= new MultiTextEdit();
		change.setEdit(multiTextEdit);

		if (mustCreateImport) {
			accessorClassName= sourceModification.createImportForAccessor(multiTextEdit, accessorClassName, accessorPackage, cu);
		}
		
		for (int i = 0; i < subs.length; i++) {
            NLSSubstitution substitution = subs[i];
            if (substitution.hasStateChanged()) {
                if (substitution.getState() == NLSSubstitution.EXTERNALIZED) {
                    if (substitution.getInitialState() == NLSSubstitution.INTERNALIZED) { 
                        sourceModification.addNLS(substitution, change, accessorClassName);
                    } else if (substitution.getInitialState() == NLSSubstitution.IGNORED) {
                        sourceModification.addAccessor(substitution, change, accessorClassName);                  
                    }
                } else if (substitution.getState() == NLSSubstitution.INTERNALIZED) {
                    if (substitution.getInitialState() == NLSSubstitution.IGNORED) {
                        sourceModification.deleteTag(substitution, change);
                    } else if (substitution.getInitialState() == NLSSubstitution.EXTERNALIZED) {
                        sourceModification.deleteAccessor(substitution, change);
                        sourceModification.deleteTag(substitution, change);
                    }
                } else if (substitution.getState() == NLSSubstitution.IGNORED) {
                    if (substitution.getInitialState() == NLSSubstitution.INTERNALIZED) {
                        sourceModification.addNLS(substitution, change, accessorClassName);                        
                    } else if (substitution.getInitialState() == NLSSubstitution.EXTERNALIZED) {
                        sourceModification.deleteAccessor(substitution, change);
                    }                    
                }
            } else if (substitution.hasChanged()) {
                if (substitution.getKey() != substitution.getInitialKey()) {
                    sourceModification.replaceKey(substitution, change);
                }
            }
        }   

		return change;
	}

    private void replaceKey(NLSSubstitution substitution, TextChange change) {
        TextRegion region = substitution.fNLSElement.getPosition();
        TextChangeCompatibility.addTextEdit(
                change, 
                "", 
                new ReplaceEdit(region.getOffset(), 
                        region.getLength(), 
                        "\"" + unwindEscapeChars(substitution.getKey()) + "\""));
    }        

    private void deleteAccessor(NLSSubstitution substitution, TextChange change) {
        AccessorClassInfo accessorClassInfo = substitution.getAccessorClassInfo();
        if (substitution.getAccessorClassInfo() != null) {
            TextChangeCompatibility.addTextEdit(
                    change, 
                    "", 
                    new ReplaceEdit(accessorClassInfo.fRegion.getOffset(), 
                            accessorClassInfo.fRegion.getLength(), 
                            "\"" + unwindEscapeChars(substitution.getValue()) + "\""));
        }           
    }
    
    // TODO: not dry
    private String unwindEscapeChars(String s){
        if (s != null) {
            StringBuffer sb= new StringBuffer(s.length());
            int length= s.length();
            for (int i= 0; i < length; i++){
                char c= s.charAt(i);
                sb.append(getUnwoundString(c));
            }
            return sb.toString();
        } 
        return null;
    }
    
    private String getUnwoundString(char c){
    	switch(c){
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

	private void addNLS(NLSSubstitution sub, TextChange change, String accessorName) {
        if (sub.getState() == NLSSubstitution.INTERNALIZED) return;
        
        String text = addAccessor(sub, change, accessorName);
        
        NLSElement element = sub.fNLSElement;
        String[] args = { text, element.getValue()};
        String name = NLSMessages.getFormattedString("NLSrefactoring.add_tag", args); //$NON-NLS-1$
        TextChangeCompatibility.addTextEdit(change, name, createAddTagChange(element));
    }
    
    private String addAccessor(NLSSubstitution sub, TextChange change, String accessorName) {
        TextRegion position = sub.fNLSElement.getPosition();
        String text = NLSMessages.getFormattedString("NLSrefactoring.extrenalize_string", sub.fNLSElement.getValue()); //$NON-NLS-1$
        
        if (sub.getState() == NLSSubstitution.EXTERNALIZED) {
            String resourceGetter = createResourceGetter(sub.getKey(), accessorName);
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
