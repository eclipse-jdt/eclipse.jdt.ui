package org.eclipse.jdt.internal.corext.refactoring.util;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.Templates;
import org.eclipse.jdt.internal.corext.template.java.JavaContext;

public class TemplateUtil {
	
	private TemplateUtil(){
	}
	
	public static String createTypeCommentSource(ICompilationUnit newCu) throws CoreException {
		return getTemplate("typecomment", 0, newCu);//$NON-NLS-1$
	}

	public static String createFileCommentsSource(ICompilationUnit newCu) throws CoreException {
		return getTemplate("filecomment", 0, newCu);//$NON-NLS-1$
	}

	private static String getTemplate(String name, int pos, ICompilationUnit newCu) throws CoreException {
		Template[] templates= Templates.getInstance().getTemplates(name);
		if (templates.length == 0)
			return ""; //$NON-NLS-1$	
		String template= JavaContext.evaluateTemplate(templates[0], newCu, pos);
		if (template == null)
			return ""; //$NON-NLS-1$
		return template;
	}

}
