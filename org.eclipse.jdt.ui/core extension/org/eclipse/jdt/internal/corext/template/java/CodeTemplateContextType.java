package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.TemplateContext;

/**
  */
public class CodeTemplateContextType extends ContextType {
	
	public CodeTemplateContextType() {
		super("codetemplate");
		
		// global
		addVariable(new GlobalVariables.Dollar());
		addVariable(new GlobalVariables.Date());
		addVariable(new GlobalVariables.Year());
		addVariable(new GlobalVariables.Time());
		addVariable(new GlobalVariables.User());
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.ContextType#createContext()
	 */
	public TemplateContext createContext() {
		return new CodeTemplateTemplateContext(this, "\n");
	}

}
