package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.ContextTypeRegistry;
import org.eclipse.jdt.internal.corext.template.ITemplateEditor;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplateTranslator;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
  */
public class CodeTemplateTemplateContext extends TemplateContext {
	
	private String fLineDelimiter;
	
	public CodeTemplateTemplateContext(ContextType type, String lineDelim) {
		super(type);
		fLineDelimiter= lineDelim;
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.TemplateContext#evaluate(org.eclipse.jdt.internal.corext.template.Template)
	 */
	public TemplateBuffer evaluate(Template template) throws CoreException {

		if (!canEvaluate(template))
			return null;
		
		TemplateTranslator translator= new TemplateTranslator();
		TemplateBuffer buffer= translator.translate(template.getPattern());

		getContextType().edit(buffer, this);

		IPreferenceStore prefs= JavaPlugin.getDefault().getPreferenceStore();
		boolean useCodeFormatter= prefs.getBoolean(PreferenceConstants.TEMPLATES_USE_CODEFORMATTER);			

		ITemplateEditor formatter= new JavaFormatter(fLineDelimiter, useCodeFormatter);
		formatter.edit(buffer, this);

		return buffer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.TemplateContext#canEvaluate(org.eclipse.jdt.internal.corext.template.Template)
	 */
	public boolean canEvaluate(Template template) {
		return true;
	}

	/**
	 * Evaluates a codetemplate in the context of a compilation unit
	 */
	public static String evaluateTemplate(Template template, ICompilationUnit compilationUnit, int position) throws CoreException {

		ContextType contextType= ContextTypeRegistry.getInstance().getContextType("codetemplate"); //$NON-NLS-1$
		if (contextType == null)
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, JavaTemplateMessages.getString("CodeTemplateTemplateContext.error.message"), null)); //$NON-NLS-1$

		IDocument document= new Document();
		if (compilationUnit != null && compilationUnit.exists())
			document.set(compilationUnit.getSource());

		CodeTemplateTemplateContext context= new CodeTemplateTemplateContext(contextType, StubUtility.getLineDelimiterUsed(compilationUnit));

		TemplateBuffer buffer= context.evaluate(template);
		return buffer.getString();
	}

}
