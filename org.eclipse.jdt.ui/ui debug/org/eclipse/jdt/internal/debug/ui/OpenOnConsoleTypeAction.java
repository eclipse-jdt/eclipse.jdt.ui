package org.eclipse.jdt.internal.debug.ui;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.util.TypeInfo;
import org.eclipse.jdt.internal.ui.util.TypeInfoLabelProvider;
import org.eclipse.jdt.internal.ui.util.TypeInfoRequestor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * This action opens a Java type in an editor based on output in the console.
 * There are two ways this can happen.  (1) if there is an explicit selection
 * in the console, the selected text is parsed for a type name (2) if there is
 * no selection, but the cursor is placed on a line of output in the console, 
 * the entire line is parsed for a type name.  
 * Example:
 *		If the cursor is placed on the following line of output in the console:
 *			at com.foo.bar.Application.run(Application.java:58)
 * 		An editor for the type com.foo.bar.Application will be opened,
 * 		and line 58 will be selected and revealed.  Note that if the word
 * 		'Application' had been selected, then the user would have been prompted
 * 		to choose a fully qualified instance of 'Application' (if
 * 		there were more than one in the workspace), and an editor opened revealing 
 * 		the beginning of the type.
 */
public class OpenOnConsoleTypeAction extends Action implements IViewActionDelegate {
																	
	private IViewPart fViewPart;
	
	private String fPkgName;
	private String fTypeName;
	private int fLineNumber;
	
	/**
	 * @see IViewActionDelegate#init(IViewPart)
	 */
	public void init(IViewPart view) {		
		fViewPart = view;	
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		doOpenType();
	}
	
	/**
	 * @see Action#run()
	 */
	public void run() {
		doOpenType();
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	protected void doOpenType() {		
		// determine what we're searching for
		fPkgName = null;
		fTypeName = null;
		fLineNumber = -1;
		determineSearchParameters();
		if (fTypeName == null) {
			return;
		}
		
		// convert package & type names to form required by SearchEngine API
		char[] typeCharArray = fTypeName.toCharArray();
		char[] pkgCharArray;
		if (fPkgName != null) {
			pkgCharArray = fPkgName.toCharArray();
		} else {
			pkgCharArray = null;
		}
					
		// construct the rest of the search parameters
		ArrayList typeRefsFound= new ArrayList(3);
		ITypeNameRequestor requestor= new TypeInfoRequestor(typeRefsFound);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IJavaSearchScope scope = SearchEngine.createWorkspaceScope();		
		
		// ask the SearchEngine to do the search
		SearchEngine engine = new SearchEngine();
		try {
			engine.searchAllTypeNames(workspace, 
			                          pkgCharArray, 
			                          typeCharArray,  
			                          IJavaSearchConstants.EXACT_MATCH, 
			                          true, 
			                          IJavaSearchConstants.TYPE,
			                          scope, 
			                          requestor, 
			                          IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, 
			                          null);
			              
		} catch (JavaModelException jme) {			
			JavaPlugin.log(jme);
			ErrorDialog.openError(getShell(), "Error searching for type", null, jme.getStatus());
		}				
		
		// choose the appropriate result             
		TypeInfo typeInfo = selectTypeInfo(typeRefsFound);			                          		
		if (typeInfo == null) {
			return;
		}
		
		// get the actual type and open an editor on it
		try {
			IType type = typeInfo.resolveType(scope);
			openAndPositionEditor(type);		
		} catch (JavaModelException jme) {
			JavaPlugin.log(jme);
			ErrorDialog.openError(getShell(), "Error searching for type", null, jme.getStatus());			
		}
	}
	
	/**
	 * Return one of the TypeInfo objects in the List argument.
	 */
	protected TypeInfo selectTypeInfo(List typeInfoList) {
		if (typeInfoList.isEmpty()) {
			return null;
		}
		if (typeInfoList.size() == 1) {
			return (TypeInfo)typeInfoList.get(0);
		}

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), 
													new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_FULLYQUALIFIED | TypeInfoLabelProvider.SHOW_ROOT_POSTFIX));
		dialog.setTitle("Open Type");
		dialog.setMessage("Choose a type to open"); 
		dialog.setElements(typeInfoList.toArray(new TypeInfo[typeInfoList.size()]));
		if (dialog.open() == dialog.OK) {
			return (TypeInfo) dialog.getFirstResult();
		}
		return null;		
	}
	
	/**
	 * Open an editor on the specified Java type.  If a line number is 
	 * available, also make the editor reveal it.
	 */
	protected void openAndPositionEditor(IType type) {
		try {
			IEditorPart editor = EditorUtility.openInEditor(type);
			if ((editor instanceof ITextEditor)  && (fLineNumber > 0)) {
				int zeroBasedLineNumber = fLineNumber - 1;
				ITextEditor textEditor = (ITextEditor) editor;
				IEditorInput input = textEditor.getEditorInput();
				IDocumentProvider provider = textEditor.getDocumentProvider();
				IDocument document = provider.getDocument(input);
				int lineOffset = document.getLineOffset(zeroBasedLineNumber);
				int lineLength = document.getLineLength(zeroBasedLineNumber);
				textEditor.selectAndReveal(lineOffset, lineLength);
			}	
		} catch (JavaModelException jme) {
			JavaPlugin.log(jme);
			ErrorDialog.openError(getShell(), "Error opening editor", null, jme.getStatus()); 			
		} catch (PartInitException pie) {
			JavaPlugin.log(pie);
			ErrorDialog.openError(getShell(), "Error opening editor", null, pie.getStatus()); 						
		} catch (BadLocationException ble) {
			JavaPlugin.log(ble);
			MessageDialog.openError(getShell(), "Error parsing console document", ble.getMessage());
		} 
	}
	
	/**
	 * Parse text in the console for a fully qualified type name and a line number. 
	 * The package qualification and line number are optional, the type name is not.
	 * The input for parsing is either an explicit selection in the console, or is
	 * the entire line where the cursor is currently located.
	 */
	protected void determineSearchParameters() {
		ISelectionProvider selectionProvider = fViewPart.getViewSite().getSelectionProvider();
		if (selectionProvider == null) {
			return;
		}		
		ISelection selection = selectionProvider.getSelection();
		if (selection instanceof ITextSelection) {
			ITextSelection textSelection = (ITextSelection)selection;
			if (textSelection.getLength() > 0) {
				parseSelection(textSelection.getText());
			} else {
				IDocument consoleDocument = DebugUIPlugin.getCurrentConsoleDocument();
				if (consoleDocument == null) {
					return;
				}
				try {
					int offset = textSelection.getOffset();
					int lineNumber = consoleDocument.getLineOfOffset(offset);
					int lineOffset = consoleDocument.getLineOffset(lineNumber);
					int lineLength = consoleDocument.getLineLength(lineNumber);		
					String lineText = consoleDocument.get(lineOffset, lineLength);				
					parseSelection(lineText);
				} catch (BadLocationException ble) {
					JavaPlugin.log(ble);
					MessageDialog.openError(getShell(), "Error parsing console document", ble.getMessage());
				}
			}
		}		
	}
	
	/**
	 * Parse out the package name (if there is one), type name and line number
	 * (if there is one).  
	 */
	protected void parseSelection(String sel) {
		// initialize
		String selection = sel.trim();
		if (selection.length() < 1) {
			return;
		}
		int leftEdge = 0;
		int firstDot = selection.indexOf('.');
		int firstParen = selection.indexOf('(');
		int rightEdge = selection.length();
		
		// isolate left edge
		if (firstDot != -1) {
			String substring = selection.substring(0, firstDot);
			leftEdge = substring.lastIndexOf(' ') + 1;
		}	
		
		// isolate right edge
		if (firstParen != -1) {
			String substring = selection.substring(leftEdge, firstParen);
			rightEdge = substring.lastIndexOf('.');
			if (rightEdge == -1) {
				rightEdge = selection.length();
			} else {
				rightEdge += leftEdge;
			}
		}
		
		// extract the fully qualified type name
		String qualifiedName = selection.substring(leftEdge, rightEdge);
		
		// extract package name and the simple type name
		int lastPkgDot = qualifiedName.lastIndexOf('.');
		if (lastPkgDot == -1) {
			fPkgName = null;
			fTypeName = qualifiedName;
		} else {
			fPkgName = qualifiedName.substring(0, lastPkgDot);
			fTypeName = qualifiedName.substring(lastPkgDot + 1, qualifiedName.length());
		}
		
		// look for line #
		int lastColon = selection.lastIndexOf(':');
		if (lastColon != -1) {
			StringBuffer buffer = new StringBuffer();
			for (int i = lastColon + 1; i < selection.length(); i++) {
				char character = selection.charAt(i);
				if (Character.isDigit(character)) {
					buffer.append(character);
				} else {
					try {
						fLineNumber = Integer.parseInt(buffer.toString());
					} catch (NumberFormatException nfe) {
						
					}
				}
			}
		}
	}

	protected Shell getShell() {
		return fViewPart.getViewSite().getShell();
	}		
	
}

