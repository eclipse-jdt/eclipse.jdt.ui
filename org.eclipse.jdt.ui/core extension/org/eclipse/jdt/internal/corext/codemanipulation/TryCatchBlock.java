/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICodeFormatter;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.template.CodeTemplates;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateTemplateContext;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class TryCatchBlock extends AbstractCodeBlock {

	private static final String TRY_PLACE_HOLDER= "f();";
	private static final String CATCH_PLACE_HOLDER= "h();";

	private AbstractCodeBlock fTryBody;
	private ITypeBinding[] fExceptions;

	public TryCatchBlock(ITypeBinding[] exceptions, AbstractCodeBlock tryBody) {
		fTryBody= tryBody;
		fExceptions= exceptions;
	}
	
	public boolean isEmpty() {
		return false;
	}

	public void fill(StringBuffer buffer, String firstLineIndent, String indent, String lineSeparator) throws CoreException {
		final String dummy= createStatement();
		ICodeFormatter formatter= ToolFactory.createCodeFormatter();
		int[] positions= computePositions(dummy);
		String formattedCode= formatter.format(dummy, 0, positions, lineSeparator);
		// begin workaround for http://dev.eclipse.org/bugs/show_bug.cgi?id=19335
		if (!adjustPositions(formattedCode, positions)) {
			// nothing we can do here.
			Assert.isTrue(false, "This should never happend");	//$NON-NLS-1$
		}
		// end workaround for http://dev.eclipse.org/bugs/show_bug.cgi?id=19335
		TextBuffer textBuffer= TextBuffer.create(formattedCode);
		String placeHolderLine= textBuffer.getLineContentOfOffset(positions[0]);
		String bodyIndent= indent + (placeHolderLine != null ? CodeFormatterUtil.createIndentString(placeHolderLine) : ""); //$NON-NLS-1$
		
		fill(buffer, formattedCode.substring(0, positions[0]), firstLineIndent, indent, lineSeparator);
		fTryBody.fill(buffer, "", bodyIndent, lineSeparator); //$NON-NLS-1$
		for (int i= 0; i < fExceptions.length; i++) {
			fill(buffer, formattedCode.substring(positions[i * 2 + 1] + 1, positions[i * 2 + 2]), "", indent, lineSeparator); //$NON-NLS-1$
			fill(buffer, getCatchBody(fExceptions[i], lineSeparator), "", indent, lineSeparator); //$NON-NLS-1$
		}
		fill(buffer, formattedCode.substring(positions[positions.length - 1] + 1), "", indent, lineSeparator); //$NON-NLS-1$
	}

	private String getCatchBody(ITypeBinding binding, String lineSeparator) throws CoreException {
//		CodeTemplates templates= CodeTemplates.getInstance();
//		Template template= templates.getTemplates("catchblock")[0];
//		CodeTemplateTemplateContext context= new CodeTemplateTemplateContext(new CodeTemplateContextType(), lineSeparator);
//		TemplateBuffer buffer= context.evaluate(template);
//		return buffer.getString();
		return "//TODO";
	}

	private int[] computePositions(final String dummy) {
		int[] result= new int[2 + fExceptions.length * 2];
		int tryPlaceHolderStart= dummy.indexOf(TRY_PLACE_HOLDER);
		result[0]= tryPlaceHolderStart; 
		result[1]= tryPlaceHolderStart + TRY_PLACE_HOLDER.length() - 1;
		int catchPlaceHolderStart= tryPlaceHolderStart;
		for (int i= 0; i < fExceptions.length; i++) {
			catchPlaceHolderStart= dummy.indexOf(CATCH_PLACE_HOLDER, catchPlaceHolderStart + 1);
			result[i * 2 + 2]= catchPlaceHolderStart;
			result[i * 2 + 3]= catchPlaceHolderStart + CATCH_PLACE_HOLDER.length() - 1;
		}
		return result;
	}

	private String createStatement() {
		StringBuffer buffer= new StringBuffer();
		buffer.append("try {f();} "); //$NON-NLS-1$
		for (int i= 0; i < fExceptions.length; i++) {
			buffer.append("catch("); //$NON-NLS-1$
			buffer.append(fExceptions[i].getName());
			buffer.append(" e){h();}"); //$NON-NLS-1$
		}
		return buffer.toString();	
	}

	/*
	 * Workaround for http://dev.eclipse.org/bugs/show_bug.cgi?id=19335
	 */
	private  boolean adjustPositions(String code, int[] positions) {
		int candidate= code.indexOf(TRY_PLACE_HOLDER);
		if (candidate == -1)
			return false;
		positions[0]= candidate;
		positions[1]= candidate + TRY_PLACE_HOLDER.length() - 1;
		
		for (int i= 0; i < fExceptions.length; i++) {
			candidate= code.indexOf(CATCH_PLACE_HOLDER, candidate + 1);
			if (candidate == -1)
				return false;
			positions[i * 2 + 2]= candidate;
			positions[i * 2 + 3]= candidate + CATCH_PLACE_HOLDER.length() - 1;
		}
		return true;
	}
}
