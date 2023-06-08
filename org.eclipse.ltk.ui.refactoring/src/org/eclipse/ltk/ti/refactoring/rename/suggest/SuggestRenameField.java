package org.eclipse.ltk.ti.refactoring.rename.suggest;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.viewers.ISelection;

import org.eclipse.jface.text.ITextSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.NamingConventions;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class SuggestRenameField {
	private ITextSelection selection;

	private static int textLength;

	private static int textStartLine;

	private static int textOffset;

	private static String textSelect;

	static List<FieldDeclaration> fieldDeclarations= new ArrayList<>();

	public static String getFieldNameSuggestions(ISelection sel, ExecutionEvent event) throws Exception {
		handleCommand(sel);
		IEditorPart editorPart1= HandlerUtil.getActiveEditor(event);
		IFile file= editorPart1.getEditorInput().getAdapter(IFile.class);
		IJavaProject javaProject= JavaCore.create(file.getProject());
		ICompilationUnit compilationUnit= JavaCore.createCompilationUnitFrom(file);
		ASTParser astParser= ASTParser.newParser(AST.JLS20);
		astParser.setKind(ASTParser.K_COMPILATION_UNIT);
		astParser.setResolveBindings(true);
		astParser.setBindingsRecovery(true);
		astParser.setSource(compilationUnit);
		CompilationUnit astRoot= (CompilationUnit) astParser.createAST(null);

		return getFieldName(astRoot, javaProject);
	}

	public static String getFieldName(CompilationUnit cu, IJavaProject javaProject) {
		String fieldName= ""; //$NON-NLS-1$
		if (!fieldDeclarations.isEmpty()) {
			fieldDeclarations.clear();
		}
		getFieldDeclaration(cu, fieldDeclarations);
		String[] string= new String[fieldDeclarations.size()];
		for (int i= 0; i < fieldDeclarations.size(); i++) {
			FieldDeclaration fd= fieldDeclarations.get(i);
			VariableDeclarationFragment vdf= (VariableDeclarationFragment) fd.fragments().get(0);
			fieldName= vdf.getName().getIdentifier();
			string[i]= fieldName;
			if (fieldName.equals(textSelect)) {
				Type fieldType= fd.getType();
				if (fieldType.toString().equals("float") || fieldType.toString().equals("int") //$NON-NLS-1$ //$NON-NLS-2$
						|| fieldType.toString().equals("char") || fieldType.toString().equals("boolean") //$NON-NLS-1$ //$NON-NLS-2$
						|| fieldType.toString().equals("long") || fieldType.toString().equals("double") //$NON-NLS-1$ //$NON-NLS-2$
						|| fieldType.toString().equals("String") || fieldType.toString().equals("byte")) { //$NON-NLS-1$ //$NON-NLS-2$
					return textSelect;
				} else {
					int modifier= fd.getModifiers();
					if (vdf.getInitializer() != null) {
						String[] suggestedNames= getFieldNameSuggestions(javaProject, fieldType.toString(), 0, modifier, string);
						if (suggestedNames.length > 0) {
							return suggestedNames[suggestedNames.length-1];
						}
                } else {
						String[] suggestedNames= getFieldNameSuggestions(javaProject, fieldType.toString(), 0, modifier, string);
						if (suggestedNames.length > 0) {
							return suggestedNames[0];
						}
					}
				}
			}
		}
		return textSelect;

	}

	public static String[] getFieldNameSuggestions(IJavaProject project, String baseName, int dimensions, int modifiers, String[] excluded) {
		if (Flags.isFinal(modifiers) && Flags.isStatic(modifiers)) {
			return getVariableNameSuggestions(NamingConventions.VK_STATIC_FINAL_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
		} else if (Flags.isStatic(modifiers)) {
			return getVariableNameSuggestions(NamingConventions.VK_STATIC_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
		}
		return getVariableNameSuggestions(NamingConventions.VK_INSTANCE_FIELD, project, baseName, dimensions, new ExcludedCollection(excluded), true);
	}

	public static String[] getVariableNameSuggestions(int variableKind, IJavaProject project, String baseName, int dimensions, Collection<String> excluded, boolean evaluateDefault) {
		return NamingConventions.suggestVariableNames(variableKind, NamingConventions.BK_TYPE_NAME, removeTypeArguments(baseName), project, dimensions, getExcludedArray(excluded), evaluateDefault);
	}

	private static String removeTypeArguments(String baseName) {
		int idx= baseName.indexOf('<');
		if (idx != -1) {
			return baseName.substring(0, idx);
		}
		return baseName;
	}

	private static String[] getExcludedArray(Collection<String> excluded) {
		if (excluded == null) {
			return null;
		} else if (excluded instanceof ExcludedCollection) {
			return ((ExcludedCollection) excluded).getExcludedArray();
		}
		return excluded.toArray(new String[excluded.size()]);
	}

	public static void handleCommand(ISelection sel) throws Exception {
		String str= sel.toString();
		String[] splitStr= str.split("[, :]"); //$NON-NLS-1$
		setTextOffset(Integer.parseInt(splitStr[3]));
		setTextStartLine(Integer.parseInt(splitStr[7]));
		setTextLength(Integer.parseInt(splitStr[11]));
		textSelect= splitStr[15];
	}

	public static IJavaProject findJavaProject(String projectName) {
		IProject[] projects= ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i= 0; i < projects.length; ++i) {
			if (JavaCore.create(projects[i]).getPath().lastSegment().contains(projectName)) {
				return JavaCore.create(projects[i]);
			}
		}
		return null;
	}

	public static IProject getProject(ExecutionEvent event) {
		IProject project= null;
		IEditorPart part= HandlerUtil.getActiveEditor(event);
		if (part != null) {
			Object object= part.getEditorInput().getAdapter(IFile.class);
			if (object != null) {
				project= ((IFile) object).getProject();
			}
		}
		return project;
	}

	public static void getFieldDeclaration(ASTNode cu, final List<FieldDeclaration> types) {
		cu.accept(new ASTVisitor() {
			@Override
			public boolean visit(FieldDeclaration node) {
				types.add(node);
				return true;
			}
		});
	}

	public static int getTextLength() {
		return textLength;
	}

	public static void setTextLength(int textLength) {
		SuggestRenameField.textLength= textLength;
	}

	public ITextSelection getSelection() {
		return selection;
	}

	public void setSelection(ITextSelection selection) {
		this.selection= selection;
	}

	public static int getTextStartLine() {
		return textStartLine;
	}

	public static void setTextStartLine(int textStartLine) {
		SuggestRenameField.textStartLine= textStartLine;
	}

	public static int getTextOffset() {
		return textOffset;
	}

	public static void setTextOffset(int textOffset) {
		SuggestRenameField.textOffset= textOffset;
	}

	private static class ExcludedCollection extends AbstractList<String> {
		private String[] fExcluded;

		public ExcludedCollection(String[] excluded) {
			fExcluded= excluded;
		}

		public String[] getExcludedArray() {
			return fExcluded;
		}

		@Override
		public int size() {
			return fExcluded.length;
		}

		@Override
		public String get(int index) {
			return fExcluded[index];
		}

		@Override
		public int indexOf(Object o) {
			if (o instanceof String) {
				for (int i= 0; i < fExcluded.length; i++) {
					if (o.equals(fExcluded[i]))
						return i;
				}
			}
			return -1;
		}

		@Override
		public boolean contains(Object o) {
			return indexOf(o) != -1;
		}
	}
}
