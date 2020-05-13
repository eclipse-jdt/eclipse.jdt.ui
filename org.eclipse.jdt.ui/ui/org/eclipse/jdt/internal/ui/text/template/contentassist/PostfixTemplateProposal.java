/*******************************************************************************
 * Copyright (c) 2019 Nicolaj Hoess.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Nicolaj Hoess - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.template.contentassist;

import org.eclipse.swt.graphics.Image;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;

import org.eclipse.jdt.internal.corext.template.java.JavaPostfixContext;

/**
 * This is an extension to the existing {@link TemplateProposal} class. <br/>
 * The class overrides the method {@link #validate(IDocument, int, DocumentEvent)} to allow the
 * replacement of existing input.
 */
public class PostfixTemplateProposal extends TemplateProposal {

	public PostfixTemplateProposal(Template template, TemplateContext context,
			IRegion region, Image image) {
		super(template, context, region, image);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.text.template.contentassist.TemplateProposal#validate(org.eclipse.jface.text.IDocument, int, org.eclipse.jface.text.DocumentEvent)
	 */
	@Override
	public boolean validate(IDocument document, int offset, DocumentEvent event) {
		if (getContext() instanceof JavaPostfixContext) {
			JavaPostfixContext c= (JavaPostfixContext) getContext();
			try {
				int start= c.getStart() + c.getAffectedSourceRegion().getLength() + 1;
				String content= document.get(start, offset - start);
				return this.getTemplate().getName().toLowerCase().startsWith(content.toLowerCase());
			} catch (BadLocationException e) {
				// fall back to parent validation
			}
		}
		return super.validate(document, offset, event);
	}
}
