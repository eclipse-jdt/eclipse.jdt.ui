package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.TemplateContext;
import org.eclipse.jdt.internal.corext.template.TemplateVariable;

/**
  */
public class CodeTemplateContextType extends ContextType {
	
	public static final String CATCHBLOCK_CONTEXTTYPE= "catchblock_context";
	public static final String METHODBODY_CONTEXTTYPE= "methodbody_context";
	public static final String NEWTYPE_CONTEXTTYPE= "newtype_context";
	
	public static final String CATCHBLOCK_NAME= "catchblock"; //$NON-NLS-1$
	public static final String EXCEPTION_TYPE= "exception_type"; //$NON-NLS-1$
	public static final String EXCEPTION_VAR= "exception_var"; //$NON-NLS-1$
	
	public static class CodeTemplateVariable extends TemplateVariable {
		public CodeTemplateVariable(String name, String description) {
			super(name, description);
		}
		
		public String evaluate(TemplateContext context) {
			if (context instanceof CodeTemplateContext) {
				return ((CodeTemplateContext) context).getVariableValue(getName());
			}
			return null;
		}
	}
		
	protected static class Todo extends TemplateVariable {

		public Todo() {
			super("todo", JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.todo")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		public String evaluate(TemplateContext context) {
			CodeTemplateContext cc= (CodeTemplateContext)context;
			
			IJavaProject javaProject= cc.getJavaProject();
			String todoTaskTag= StubUtility.getTodoTaskTag(javaProject);
			if (todoTaskTag == null)
				return "XXX"; //$NON-NLS-1$
	
			return todoTaskTag;
		}
	}
		
	
	public CodeTemplateContextType(String contextName) {
		super(contextName);
		
		// global
		addVariable(new GlobalVariables.Dollar());
		addVariable(new GlobalVariables.Date());
		addVariable(new GlobalVariables.Year());
		addVariable(new GlobalVariables.Time());
		addVariable(new GlobalVariables.User());
		addVariable(new Todo());	
		
		if (CATCHBLOCK_CONTEXTTYPE.equals(contextName)) {
			addVariable(new CodeTemplateVariable(EXCEPTION_TYPE,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.exceptionType")));
			addVariable(new CodeTemplateVariable(EXCEPTION_VAR,  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.exceptionVar")));
		} else if (METHODBODY_CONTEXTTYPE.equals(contextName)) {
		} else if (NEWTYPE_CONTEXTTYPE.equals(contextName)) {
			addVariable(new CodeTemplateVariable("package_statement",  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.packstatement")));
			addVariable(new CodeTemplateVariable("type_declaration",  JavaTemplateMessages.getString("CodeTemplateContextType.variable.description.typedeclaration")));
		}
	}
	

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.corext.template.ContextType#createContext()
	 */
	public TemplateContext createContext() {
		return null;
	}

}
