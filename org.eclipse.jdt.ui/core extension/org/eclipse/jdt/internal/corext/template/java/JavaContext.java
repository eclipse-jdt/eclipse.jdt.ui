/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template.java;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.codemanipulation.ImportsStructure;
import org.eclipse.jdt.internal.corext.template.ContextType;
import org.eclipse.jdt.internal.corext.template.ContextTypeRegistry;
import org.eclipse.jdt.internal.corext.template.ITemplateEditor;
import org.eclipse.jdt.internal.corext.template.Template;
import org.eclipse.jdt.internal.corext.template.TemplateBuffer;
import org.eclipse.jdt.internal.corext.template.TemplateTranslator;
import org.eclipse.jdt.internal.corext.template.java.CompilationUnitCompletion.LocalVariable;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.util.Strings;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * A context for java source.
 */
public class JavaContext extends CompilationUnitContext {

	/** The platform default line delimiter. */
	private static final String PLATFORM_LINE_DELIMITER= System.getProperty("line.separator"); //$NON-NLS-1$

	/** A flag to force evaluation in head-less mode. */
	private boolean fForceEvaluation;
	/** A code completion requestor for guessing local variable names. */
	private CompilationUnitCompletion fCompletion;
	
	/**
	 * Creates a java template context.
	 * 
	 * @param type   the context type.
	 * @param document the document.
	 * @param completionPosition the completion position within the document.
	 * @param unit the compilation unit (may be <code>null</code>).
	 */
	public JavaContext(ContextType type, IDocument document, int completionPosition,
		ICompilationUnit compilationUnit)
	{
		super(type, document, completionPosition, compilationUnit);
	}
	
	/*
	 * @see TemplateContext#evaluate(Template template)
	 */
	public TemplateBuffer evaluate(Template template) throws CoreException {
		if (!canEvaluate(template))
			return null;
		
		TemplateTranslator translator= new TemplateTranslator();
		TemplateBuffer buffer= translator.translate(template.getPattern());

		getContextType().edit(buffer, this);
			
		String lineDelimiter= null;
		try {
			lineDelimiter= getDocument().getLineDelimiter(0);
		} catch (BadLocationException e) {
		}

		if (lineDelimiter == null)
			lineDelimiter= PLATFORM_LINE_DELIMITER;
		
		ITemplateEditor formatter= new JavaFormatter(lineDelimiter);
		formatter.edit(buffer, this);

		return buffer;
	}
	
	/**
	 * Forces evaluation.
	 */
	public void setForceEvaluation(boolean evaluate) {
		fForceEvaluation= evaluate;	
	}
	
	/*
	 * @see TemplateContext#canEvaluate(Template templates)
	 */
	public boolean canEvaluate(Template template) {
		return fForceEvaluation || template.matches(getKey(), getContextType().getName());
	}

	/*
	 * @see DocumentTemplateContext#getCompletionPosition();
	 */
	public int getStart() {
		IDocument document= getDocument();
		try {
			int start= getCompletionPosition();
	
			while ((start != 0) && Character.isUnicodeIdentifierPart(document.getChar(start - 1)))
				start--;
				
			if ((start != 0) && Character.isUnicodeIdentifierStart(document.getChar(start - 1)))
				start--;
	
			return start;

		} catch (BadLocationException e) {
			return getCompletionPosition();	
		}
	}

	/**
	 * Returns the character before start position of completion.
	 */
	public char getCharacterBeforeStart() {
		int start= getStart();
		
		try {
			return start == 0
				? ' '
				: getDocument().getChar(start - 1);

		} catch (BadLocationException e) {
			return ' ';
		}
	}

	/**
	 * Returns the indentation level at the position of code completion.
	 */
	public int getIndentationLevel() {
		int start= getStart();

		TextBuffer textBuffer= TextBuffer.create(getDocument().get());
	    String lineContent= textBuffer.getLineContentOfOffset(start);

		return Strings.computeIndent(lineContent, CodeFormatterPreferencePage.getTabSize());
	}

	private CompilationUnitCompletion guessVariableNames() {
		ICompilationUnit unit= getCompilationUnit();
		int start= getStart();
		
		if (unit == null)
			return null;
		
		try {
			CompilationUnitCompletion collector= new CompilationUnitCompletion(unit);
			unit.codeComplete(start, collector);			
			return collector;
		
		} catch (JavaModelException e) {
			handleException(null, e);
			return null;
		}
	}	
	
	
	private static void handleException(Shell shell, Exception e) {
		String title= JavaTemplateMessages.getString("JavaContext.error.title"); //$NON-NLS-1$
		if (e instanceof CoreException)
			ExceptionHandler.handle((CoreException)e, shell, title, null);
		else if (e instanceof InvocationTargetException)
			ExceptionHandler.handle((InvocationTargetException)e, shell, title, null);
		else {
			JavaPlugin.log(e);
			MessageDialog.openError(shell, title, e.getMessage());
		}
	}	

	private CompilationUnitCompletion getCompletion() {
		ICompilationUnit compilationUnit= getCompilationUnit();
		if (fCompletion == null) {
			fCompletion= new CompilationUnitCompletion(compilationUnit);
			
			if (compilationUnit != null) {
				try {
					compilationUnit.codeComplete(getStart(), fCompletion);
				} catch (JavaModelException e) {
					// ignore
				}
			}
		}
		
		return fCompletion;
	}

	/**
	 * Returns the name of a guessed local array, <code>null</code> if no local
	 * array exists.
	 */
	public String guessArray() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
				
		if (localArrays.length > 0)
			return localArrays[localArrays.length - 1].name;

		return null;	
	}
	
	/**
	 * Returns the name of the type of a local array, <code>null</code> if no local
	 * array exists.
	 */
	public String guessArrayType() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
				
		if (localArrays.length > 0) {
			String typeName= localArrays[localArrays.length - 1].typeName;
			return typeName.substring(0, typeName.indexOf('['));
		}
		
		return null;
	}
	
	/**
	 * Returns a proposal for a variable name of a local array element, <code>null</code>
	 * if no local array exists.
	 */
	public String guessArrayElement() {
		CompilationUnitCompletion completion= getCompletion();
		LocalVariable[] localArrays= completion.findLocalArrays();
		
		if (localArrays.length > 0) {
			String typeName= localArrays[localArrays.length - 1].typeName;
			String baseTypeName= typeName.substring(0, typeName.indexOf('['));
			String variableName= completion.typeToVariable(baseTypeName);
			
			if (!completion.existsLocalName(variableName))
				return variableName;
		}

		return null;
	}

	/**
	 * Returns an array index name. 'i', 'j', 'k' are tried until no name collision with
	 * an existing local variable occurs. If all names collide, <code>null</code> is returned.
	 */	
	public String getIndex() {
		CompilationUnitCompletion completion= getCompletion();
		String[] proposals= {"i", "j", "k"};  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
		for (int i= 0; i != proposals.length; i++) {
			String proposal = proposals[i];

			if (!completion.existsLocalName(proposal))
				return proposal;
		}

		return null;
	}
	
	/**
	 * Returns the name of a local collection, <code>null</code> if no local collection
	 * exists.
	 */
	public String guessCollection() {
		CompilationUnitCompletion completion= getCompletion();
		try {
			LocalVariable[] localCollections= completion.findLocalCollections();
		
			if (localCollections.length > 0)
				return localCollections[localCollections.length - 1].name;

		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}

		return null;
	}

	/**
	 * Returns an iterator name ('iter'). If 'iter' already exists as local variable,
	 * <code>null</code> is returned.
	 */
	public String getIterator() {
		CompilationUnitCompletion completion= getCompletion();		
		String[] proposals= {"iter"}; //$NON-NLS-1$
		
		for (int i= 0; i != proposals.length; i++) {
			String proposal = proposals[i];

			if (!completion.existsLocalName(proposal))
				return proposal;
		}

		return null;
	}


	public void addIteratorImport() {
	
		try {
			CodeGenerationSettings settings= JavaPreferencesSettings.getCodeGenerationSettings();
			ImportsStructure structure= new ImportsStructure(getCompilationUnit(), settings.importOrder, settings.importThreshold, true);
			structure.addImport("java.util.Iterator"); //$NON-NLS-1$
			structure.create(false, null);

		} catch (CoreException e) {
			handleException(null, e);
		}
	}
	
	/**
	 * Evaluates a 'java' template in thecontext of a compilation unit
	 */
	public static String evaluateTemplate(Template template, ICompilationUnit compilationUnit, int position) throws CoreException {
		ContextType contextType= ContextTypeRegistry.getInstance().getContextType("java"); //$NON-NLS-1$
		if (contextType == null)
			throw new CoreException(new Status(IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR, JavaTemplateMessages.getString("JavaContext.error.message"), null)); //$NON-NLS-1$

		IDocument document= new Document();
		if (compilationUnit != null && compilationUnit.exists())
			document.set(compilationUnit.getSource());

		JavaContext context= new JavaContext(contextType, document, position, compilationUnit);
		context.setForceEvaluation(true);

		TemplateBuffer buffer= context.evaluate(template);
		return buffer.getString();
	}
	
	
}

