/*****************************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the Common Public
 * License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ****************************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls;

import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportRewrite;
import org.eclipse.jdt.internal.corext.refactoring.changes.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.jdt.internal.corext.refactoring.util.ResourceUtil;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRegion;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextChange;

class SourceModification {

	private final NLSHolder fNlsHolder;
	private final String fSubstitutionPrefix;
	private final String fSubstitutionPattern;
	private final ICompilationUnit fCu;
	private final String fAccessorClassName;
	private final IPackageFragment fAccessorPackage;
	private final String fDefaultSubstitution;

	private SourceModification(ICompilationUnit cu, NLSHolder nlsHolder, String defaultSubstitution,
		String substitutionPattern, String substitutionPrefix, IPackageFragment accessorPackage, String accessorClassName) {
		fNlsHolder= nlsHolder;
		fSubstitutionPattern= substitutionPattern;
		fDefaultSubstitution= defaultSubstitution;
		fSubstitutionPrefix= substitutionPrefix;
		fCu= cu;
		fAccessorClassName= accessorClassName;
		fAccessorPackage= accessorPackage;
	}

	public static Change create(ICompilationUnit cu, NLSHolder nlsHolder, String defaultSubstitution,
		String substitutionPattern, String substitutionPrefix, boolean willCreateAccessor, IPackageFragment accessorPackage,
		String accessorClassName) throws CoreException {

		SourceModification sourceModification= new SourceModification(cu, nlsHolder, defaultSubstitution, substitutionPattern,
			substitutionPrefix, accessorPackage, accessorClassName);

		NLSSubstitution[] subs= nlsHolder.getSubstitutions();

		String message= NLSMessages.getFormattedString("NLSRefactoring.externalize_strings", //$NON-NLS-1$
			cu.getElementName());

		TextChange change= new CompilationUnitChange(message, cu);
		MultiTextEdit multiTextEdit= new MultiTextEdit();
		change.setEdit(multiTextEdit);

		String accessorName= accessorClassName;
		if (willCreateAccessor) {
			accessorName= sourceModification.createImportForAccessor(multiTextEdit);
		}

		for (int i= 0; i < subs.length; i++) {
			sourceModification.addNLS(subs[i], change, accessorName);
		}

		return change;
	}

	private String createImportForAccessor(MultiTextEdit parent) throws CoreException {

		TextBuffer buffer= null;
		try {
			IType type= fAccessorPackage.getCompilationUnit(fAccessorClassName + ".java").getType(fAccessorClassName); //$NON-NLS-1$
			String fullyQualifiedName= type.getFullyQualifiedName();

			ImportRewrite importRewrite= new ImportRewrite(fCu);
			String nameToUse= importRewrite.addImport(fullyQualifiedName);
			buffer= TextBuffer.acquire(ResourceUtil.getFile(fCu));
			TextEdit edit= importRewrite.createEdit(buffer.getDocument());
			parent.addChild(edit);

			return nameToUse;
		} finally {
			if (buffer != null) {
				TextBuffer.release(buffer);
			}
		}

	}

	private void addNLS(NLSSubstitution sub, TextChange builder, String accessorName) {
		if (sub.task == NLSSubstitution.SKIP)
			return;

		TextRegion position= sub.value.getPosition();
		String text= NLSMessages.getFormattedString("NLSrefactoring.extrenalize_string", sub.value.getValue()); //$NON-NLS-1$

		if (sub.task == NLSSubstitution.TRANSLATE) {
			String resourceGetter= createResourceGetter(sub.getKeyWithPrefix(fSubstitutionPrefix), accessorName);
			TextChangeCompatibility.addTextEdit(builder, text, new ReplaceEdit(position.getOffset(), position.getLength(),
				resourceGetter));
		}

		NLSElement element= sub.value;
		String[] args= {text, element.getValue()};
		String name= NLSMessages.getFormattedString("NLSrefactoring.add_tag", args); //$NON-NLS-1$
		TextChangeCompatibility.addTextEdit(builder, name, createAddTagChange(element));
	}

	private TextEdit createAddTagChange(NLSElement element) {
		int offset= element.getTagPosition().getOffset(); //to be changed
		String text= createTagText(element);
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

	//XXX performance improvement oportunities here
	private NLSLine findLine(NLSElement element) {
		NLSLine[] lines= fNlsHolder.getLines();
		for (int i= 0; i < lines.length; i++) {
			NLSElement[] lineElements= lines[i].getElements();
			for (int j= 0; j < lineElements.length; j++) {
				if (lineElements[j].equals(element)) {
					return lines[i];
				}
			}
		}
		return null;
	}

	private int computeTagIndex(NLSElement element) {
		NLSLine line= findLine(element);
		Assert.isNotNull(line, "line not found for:" + element); //$NON-NLS-1$
		return computeIndexInLine(element, line) + 1; //tags are 1 based
	}

	private String createTagText(NLSElement element) {
		return " " + NLSElement.createTagText(computeTagIndex(element)); //$NON-NLS-1$
	}

	private int computeIndexInLine(NLSElement element, NLSLine line) {
		for (int i= 0; i < line.size(); i++) {
			if (line.get(i).equals(element))
				return i;
		}
		Assert.isTrue(false, "element not found in line"); //$NON-NLS-1$
		return -1;
	}
}