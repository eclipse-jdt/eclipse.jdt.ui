/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.textmanipulation.TextUtil;
import org.eclipse.jdt.internal.formatter.CodeFormatter;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CodeFormatterPreferencePage;
import org.eclipse.jdt.internal.ui.preferences.TemplatePreferencePage;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionManager;
import org.eclipse.jdt.internal.ui.text.link.LinkedPositionUI;

public class TemplateEvaluator implements VariableEvaluator, LinkedPositionUI.ExitListener {

	private static final String TEMPLATE_POSITION= "TemplateEvaluator.template.position";	
	private static final String VARIABLE_POSITION= "TemplateEvaluator.variable.position";
	private static final String MARKER= "/*${cursor}*/";
	private static final IPositionUpdater fgVariableUpdater= new DefaultPositionUpdater(VARIABLE_POSITION);
	private static final IPositionUpdater fgTemplateUpdater= new DefaultPositionUpdater(TEMPLATE_POSITION);

	private TemplateInterpolator fInterpolator= new TemplateInterpolator();

	private Template fTemplate;
	private TemplateContext fContext;

	private IDocument fDocument;	
	private String fOldText;
	
	public TemplateEvaluator(Template template, TemplateContext context) {
		fTemplate= template;
		fContext= context;
	}
	
	/*
	 * @see VariableEvaluator#reset()
	 */
	public void reset() {
		fDocument= new Document();
		fDocument.addPositionCategory(VARIABLE_POSITION);
	}

	/*
	 * @see VariableEvaluator#acceptError(String)
	 */
	public void acceptError(String message) {
	}

	/*
	 * @see VariableEvaluator#acceptText(String)
	 */
	public void acceptText(String text) {
		try {
			int offset= fDocument.getLength();
			fDocument.replace(offset, 0, text);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
			openErrorDialog(e);
		}
	}

	/*
	 * @see VariableEvaluator#acceptVariable(String)
	 */
	public void acceptVariable(String variable) {
		try {
			int offset= fDocument.getLength();
			
			if (variable.equals("cursor")) {
				fDocument.addPosition(VARIABLE_POSITION, new TypedPosition(offset, 0, variable));
				return;
			} else {				
				fDocument.replace(offset, 0, variable);				
				fDocument.addPosition(VARIABLE_POSITION, new TypedPosition(offset, variable.length(), variable));
				return;
			}
		} catch (BadLocationException e) {
			JavaPlugin.log(e);			
			openErrorDialog(e);
		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
			Assert.isTrue(false);
		}
	}	

	public String evaluate() {
		return getDocument(0).get();
	}
	
	private IDocument getDocument(int indentationLevel) {
		String pattern= fTemplate.getPattern();
		fInterpolator.interpolate(pattern, this);

		fDocument.addPositionUpdater(fgVariableUpdater);
		
		format(indentationLevel);
		guessVariableNames();

		return fDocument;	
	}

	private void format(int indentationLevel) {
		try {
			if (TemplatePreferencePage.useCodeFormatter() &&
				fContext.getType().equals(TemplateContext.JAVA))
			{
				format2(indentationLevel);
			} else {
				indentate(indentationLevel);
			}
			trimBegin();
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
			openErrorDialog(e);
		}
	}

	private Position[] getPositions() {
		try {
			return fDocument.getPositions(VARIABLE_POSITION);
		} catch (BadPositionCategoryException e) {
			Assert.isTrue(false);
			return null;
		}
	}

	private int[] getOffsets() {
		Position[] positions= getPositions();
				
		int[] offsets= new int[positions.length];
		for (int i= 0; i != positions.length; i++)
			offsets[i]= positions[i].getOffset();

		return offsets;
	}
	
	private Position getCaretPosition() {
		Position[] positions= getPositions();
		
		for (int i= 0; i != positions.length; i++) {
			if (((TypedPosition) positions[i]).getType().equals("cursor"))
				return positions[i];
		}

		return null;
	}
	
	private int getCaretOffset() {
		Position position= getCaretPosition();
		
		if (position == null)
			return fDocument.getLength();
		else
			return position.getOffset();		
	}
	
	private void update(String string, int[] offsets) throws BadLocationException {
		fDocument.removePositionUpdater(fgVariableUpdater);
		fDocument.replace(0, fDocument.getLength(), string);
		fDocument.addPositionUpdater(fgVariableUpdater);

		try {		
			// update positions ourselves
			Position[] positions= fDocument.getPositions(TemplateEvaluator.VARIABLE_POSITION);
			Assert.isTrue(positions.length == offsets.length);
			for (int i= 0; i != positions.length; i++)
				positions[i].setOffset(offsets[i]);	
		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
			Assert.isTrue(false);
		}
	}

	private void format2(int indentationLevel) throws BadLocationException {
		// XXX 4360
		// workaround for code formatter limitations
		// handle a special case where cursor position is surrounded by whitespaces		

		int caretOffset= getCaretOffset();

		if ((caretOffset > 0) && Character.isWhitespace(fDocument.getChar(caretOffset - 1)) &&
			(caretOffset < fDocument.getLength()) && Character.isWhitespace(fDocument.getChar(caretOffset)))
		{			
			fDocument.replace(caretOffset, 0, MARKER);
			getCaretPosition().setOffset(caretOffset); // reposition caret

			format3(indentationLevel);			
	
			caretOffset= getCaretOffset();
			fDocument.replace(caretOffset, MARKER.length(), "");			
		} else {
			format3(indentationLevel);			
		}
	}

	private void format3(int indentationLevel) throws BadLocationException {
		CodeFormatter formatter= new CodeFormatter(JavaCore.getOptions());

		int[] offsets= getOffsets();
		String formattedString= formatter.format(fDocument.get(), indentationLevel, offsets);	
		update(formattedString, offsets);
	}
	
	private void indentate(int indentationLevel) throws BadLocationException {
		CodeIndentator indentator= new CodeIndentator();			
		indentator.setIndentationLevel(indentationLevel);			
		indentator.setPositionsToMap(getOffsets());
		String indentatedString= indentator.indentate(fDocument.get());
			
		update(indentatedString, indentator.getMappedPositions());		
	}

	private void trimBegin() throws BadLocationException {
		String string= fDocument.get();

		int i= 0;
		while ((i != string.length()) && Character.isWhitespace(string.charAt(i)))
			i++;
			
		fDocument.replace(0, i, "");
	}
	
	public IRegion apply(IDocument document) {
		int start= fContext.getStart();
		int end= fContext.getEnd();
		
		try {
			int indentationLevel= guessIndentationLevel(document, start);
			IDocument template= getDocument(indentationLevel);

			// backup and replace with template
			fOldText= document.get(start, end - start);			
			document.replace(start, end - start, template.get());	

			// register template position
			document.addPositionCategory(TEMPLATE_POSITION);
			document.addPositionUpdater(fgTemplateUpdater);
			try {
				document.addPosition(TEMPLATE_POSITION, new Position(start, template.get().length()));
			} catch (BadPositionCategoryException e) {
				JavaPlugin.log(e);
				Assert.isTrue(false);	
			}

			Position[] positions= getPositions();
			LinkedPositionManager manager= new LinkedPositionManager(document);
			for (int i= 0; i != positions.length; i++) {
				TypedPosition position= (TypedPosition) positions[i];
				
				if (position.getType().equals("cursor"))
					continue;
				
				int offset= position.getOffset() + start;
				int length= position.getLength();
				
				manager.addPosition(offset, length);
			}
			
			LinkedPositionUI editor= new LinkedPositionUI(fContext.getViewer(), manager);
			editor.setFinalCaretOffset(getCaretOffset() + start);
			editor.setCancelListener(this);
			editor.enter();

			return editor.getSelectedRegion();

		} catch (BadLocationException e) {
			JavaPlugin.log(e);	
			openErrorDialog(e);
		}
		
		return null;
	}
	
	public void exit(boolean accept) {
		IDocument document= fContext.getViewer().getDocument();		

		try {
			if (!accept) {
				Position[] positions= document.getPositions(TEMPLATE_POSITION);
				Assert.isTrue((positions != null) && (positions.length == 1));
				
				int offset= positions[0].getOffset();
				int length= positions[0].getLength();
						
				document.replace(offset, length, fOldText);	
			}

			document.removePositionUpdater(fgTemplateUpdater);
			document.removePositionCategory(TEMPLATE_POSITION);		

		} catch (BadPositionCategoryException e) {
			JavaPlugin.log(e);
			Assert.isTrue(false);
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
			openErrorDialog(e);
		}
	}
	
	private static int guessIndentationLevel(IDocument document, int offset) throws BadLocationException {
		IRegion region= document.getLineInformationOfOffset(offset);
		String line= document.get(region.getOffset(), region.getLength());
		return TextUtil.getIndent(line, CodeFormatterPreferencePage.getTabSize());
	}

	private void guessVariableNames() {
		ICompilationUnit unit= fContext.getUnit();

		if (unit == null)
			return;
		
		try {
			int start= fContext.getStart();
			TemplateCollector collector= new TemplateCollector(unit);
						
			unit.codeComplete(start, collector);
			
			Position[] positions= getPositions();			
			if (positions == null)
				return;
			
			// apply guessed variable names
			for (int i= 0; i < positions.length; i++) {
				Position position= positions[i];

				int offset= position.getOffset();
				int length= position.getLength();
				
				String string= fDocument.get(offset, length);
				string= collector.evaluate(string);

				if (string != null)
					fDocument.replace(offset, length, string);
			}
		
		} catch (BadLocationException e) {
			JavaPlugin.log(e);
			openErrorDialog(e);
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
			openErrorDialog(e);
		}
	}

	private void openErrorDialog(JavaModelException e) {
		Shell shell= fContext.getViewer().getTextWidget().getShell();
		ErrorDialog.openError(shell, TemplateMessages.getString("TemplateEvaluator.error.accessing.title"), TemplateMessages.getString("TemplateEvaluator.error.accessing.message"), e.getStatus()); //$NON-NLS-2$ //$NON-NLS-1$
	}	

	private void openErrorDialog(BadLocationException e) {
		Shell shell= fContext.getViewer().getTextWidget().getShell();
		MessageDialog.openError(shell, TemplateMessages.getString("TemplateEvaluator.error.title"), e.getMessage()); //$NON-NLS-1$
	}	
	
}

