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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.ITypeBinding;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.dom.CodeScopeBuilder;
import org.eclipse.jdt.internal.corext.template.CodeTemplates;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContext;
import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.CodeFormatterUtil;

public class TryCatchBlock extends AbstractCodeBlock {

	private static final String TRY_PLACE_HOLDER= "f();"; //$NON-NLS-1$

	private AbstractCodeBlock fTryBody;
	private ITypeBinding[] fExceptions;
	private IJavaProject fJavaProject;
	private CodeScopeBuilder.Scope fScope;

	public TryCatchBlock(ITypeBinding[] exceptions, IJavaProject project, CodeScopeBuilder.Scope scope, AbstractCodeBlock tryBody) {
		fTryBody= tryBody;
		fExceptions= exceptions;
		fJavaProject= project;
		fScope= scope;
	}
	
	public boolean isEmpty() {
		return false;
	}

	public void fill(StringBuffer buffer, String firstLineIndent, String indent, String lineSeparator) throws CoreException {
		final String dummy= createStatement(lineSeparator);
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
		fill(buffer, formattedCode.substring(positions[1] + 1), "", indent, lineSeparator); //$NON-NLS-1$
	}

	private int[] computePositions(final String dummy) {
		int[] result= new int[2];
		int tryPlaceHolderStart= dummy.indexOf(TRY_PLACE_HOLDER);
		result[0]= tryPlaceHolderStart; 
		result[1]= tryPlaceHolderStart + TRY_PLACE_HOLDER.length() - 1;
		return result;
	}

	private String createStatement(String lineSeparator) throws CoreException {
		StringBuffer buffer= new StringBuffer();
		buffer.append("try {f();} "); //$NON-NLS-1$
		for (int i= 0; i < fExceptions.length; i++) {
			buffer.append("catch("); //$NON-NLS-1$
			buffer.append(fExceptions[i].getName());
			buffer.append(" "); //$NON-NLS-1$ 
			String name= fScope.createName("e", false); //$NON-NLS-1$
			buffer.append(name);  
			buffer.append("){" + lineSeparator + getCatchBody(fExceptions[i], name, lineSeparator) + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return buffer.toString();	
	}

	private String getCatchBody(ITypeBinding exception, String name, String lineSeparator) throws CoreException {
		Template template= CodeTemplates.getCodeTemplate(CodeTemplates.CATCHBLOCK);
		if (template == null) {
			return ""; //$NON-NLS-1$
		}		
		CodeTemplateContext context= new CodeTemplateContext(template.getContextTypeName(), fJavaProject, lineSeparator, 0);
		context.setVariable(CodeTemplateContextType.EXCEPTION_TYPE, exception.getName());
		context.setVariable(CodeTemplateContextType.EXCEPTION_VAR, name); //$NON-NLS-1$
		TemplateBuffer buffer= context.evaluate(template);
		if (buffer == null)
			return ""; //$NON-NLS-1$
		else
			return buffer.getString();
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
		return true;
	}
}
