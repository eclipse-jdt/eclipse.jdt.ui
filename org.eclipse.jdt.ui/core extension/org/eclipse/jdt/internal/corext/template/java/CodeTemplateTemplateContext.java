package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.corext.template.ContextType;
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

}
