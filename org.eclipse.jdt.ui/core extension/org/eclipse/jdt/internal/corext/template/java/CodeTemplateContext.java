package org.eclipse.jdt.internal.corext.template.java;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.template.CodeTemplates;
import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.ContextTypeRegistry;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplateTranslator;

/**
  */
public class CodeTemplateContext extends TemplateContext {
	
	private String fLineDelimiter;
	private int fInitialIndentLevel;
	private Map fMappedValues;
	private IJavaProject fProject;
	
	public CodeTemplateContext(ContextType type, IJavaProject project, Map mappedValues, String lineDelim, int initialIndentLevel) {
		super(type);
		fLineDelimiter= lineDelim;
		fInitialIndentLevel= initialIndentLevel;
		fMappedValues= mappedValues;
		fProject= project;
	}

	public String getVariableValue(String variableName) {
		return (String) fMappedValues.get(variableName);
	}	

	public IJavaProject getJavaProject() {
		return fProject;
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
		return buffer;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.TemplateContext#canEvaluate(org.eclipse.jdt.internal.corext.template.Template)
	 */
	public boolean canEvaluate(Template template) {
		return true;
	}

	/**
	 * Evaluates a codetemplate in the given cotext
	 */
	public static String evaluateTemplate(String templateName, IJavaProject project, Map mappedValues, String lineDelimiter, int initialIndentLevel) throws CoreException {
		Template[] templates= CodeTemplates.getInstance().getTemplates(templateName);
		if (templates == null || templates.length != 1) {
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, JavaTemplateMessages.getString("CodeTemplateContext.error.message"), null)); //$NON-NLS-1$
		}
		Template template= templates[0];

		ContextType contextType= ContextTypeRegistry.getInstance().getContextType(template.getContextTypeName()); //$NON-NLS-1$
		if (contextType == null)
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, JavaTemplateMessages.getString("CodeTemplateContext.error.message"), null)); //$NON-NLS-1$

		CodeTemplateContext context= new CodeTemplateContext(contextType, project, mappedValues, lineDelimiter, initialIndentLevel);
		TemplateBuffer buffer= context.evaluate(template);
		return buffer.getString();
	}
}
