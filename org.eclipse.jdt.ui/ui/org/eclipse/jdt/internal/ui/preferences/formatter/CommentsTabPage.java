/*
 * Created on Nov 27, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.jdt.internal.ui.preferences.formatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import org.eclipse.jdt.ui.PreferenceConstants;

/**
 * 
 */
public class CommentsTabPage extends ModifyDialogTabPage {
	
	
	private final class Controller implements Observer {
		
		private final CheckboxPreference fMaster;
		private final Collection fSlaves;
		
		public Controller(CheckboxPreference master, Collection slaves) {
			fMaster= master;
			fSlaves= slaves;
		}

		public void update(Observable o, Object arg) {
			final boolean enabled= fMaster.getChecked();
			for (final Iterator iter = fSlaves.iterator(); iter.hasNext();) {
				CheckboxPreference pref= (CheckboxPreference) iter.next();
				pref.setEnabled(enabled);
			}
		}
	}
	
	
	final int numColumns= 4;
	

	final String preview=
		"/**\n" +
		"* An example for comment formatting. This example is meant" +
		"to illustrate the various possiblilities offered by <i>Eclipse</i> " +
		"in order to format comments.\n" +
		"*\n" +
		"* These possibilities include:\n" +
		"* <ul>" +
		" <li>Formatting of header comments.</li>" +
		" <li>Formatting of Javadoc tags</li>" +
		" <li>Other features</li>" +
		" </ul>\n" +
		"*/\n" + 
		"interface Example {" +
		"/**\n*\n*\n" +
		"* Calculate the result of a foo operation on this object. The following is some sample code which illustrates" +
        "how <code>foo</code> can be used.\n" +
//		"* <pre>for(int i=0;i!=10;i++){someExample.foo(i,otherNumber);}</pre> This will ensure proper caching.\n" +
		"<pre>someExample.foo(1, 10);</pre>" + 
		"@param a The first parameter. For an optimum result, this should be an odd number\n" +
		"   * between 0 and 100. Greater numbers yield more exact results, yet have a serious impact on performance ( O(n*n))\n" +
		"   * @param b The second parameter.\n" +
		"   * @result The result of the foo operation, usually within 0 and 1000.\n" +
		"   */" +
		"  int foo(int a, int b);" +
		"}";
	
	
	public CommentsTabPage(Map workingValues) {
		super(workingValues);
		fJavaPreview.setPreviewText(preview);
	}

	
	protected Composite doCreatePreferences(Composite parent) {
		final Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(createGridLayout(numColumns, false));
		
		final Group commentGroup= createGroup(numColumns, composite, "Comment Formatting");
		
		final CheckboxPreference master= createCheckboxPref(commentGroup, numColumns, "&Format comments", PreferenceConstants.FORMATTER_COMMENT_FORMAT, falseTrue);
		
		// placeholder
		createLabel(1, commentGroup, "     ", GridData.HORIZONTAL_ALIGN_BEGINNING);
		
		final Composite slavesComposite= new Composite(commentGroup, SWT.NONE);
		slavesComposite.setLayoutData(createGridData(numColumns - 1));
		slavesComposite.setLayout(createGridLayout(numColumns, false));

		final Collection slaves= new ArrayList();
		slaves.add(createCheckboxPref(slavesComposite, numColumns, "Format &header comment", PreferenceConstants.FORMATTER_COMMENT_FORMATHEADER, falseTrue));
		slaves.add(createCheckboxPref(slavesComposite, numColumns, "Format HTML ta&gs", PreferenceConstants.FORMATTER_COMMENT_FORMATHTML, falseTrue));
		slaves.add(createCheckboxPref(slavesComposite, numColumns, "Format &Java code snippets", PreferenceConstants.FORMATTER_COMMENT_FORMATSOURCE, falseTrue));
		slaves.add(createCheckboxPref(slavesComposite, numColumns, "Clear &blank lines in comments", PreferenceConstants.FORMATTER_COMMENT_CLEARBLANKLINES, falseTrue));
		slaves.add(createCheckboxPref(slavesComposite, numColumns, "Empt&y line before Javadoc tags", PreferenceConstants.FORMATTER_COMMENT_SEPARATEROOTTAGS, falseTrue));
		slaves.add(createCheckboxPref(slavesComposite, numColumns, "New line &after @param tags", PreferenceConstants.FORMATTER_COMMENT_NEWLINEFORPARAMETER, falseTrue));
		final CheckboxPreference submaster= createCheckboxPref(slavesComposite, numColumns, "Indent Javado&c tags", PreferenceConstants.FORMATTER_COMMENT_INDENTROOTTAGS, falseTrue);
		slaves.add(submaster);
		final Collection subslaves= new ArrayList();
		final CheckboxPreference subslave= createCheckboxPref(slavesComposite, numColumns, "Indent de&scription after @param", PreferenceConstants.FORMATTER_COMMENT_INDENTPARAMETERDESCRIPTION, falseTrue);
		slaves.add(subslave);
		subslaves.add(subslave);
		
		master.addObserver(new Controller(master, slaves));
		submaster.addObserver(new Controller(submaster, subslaves));
		return composite;
	}
}
