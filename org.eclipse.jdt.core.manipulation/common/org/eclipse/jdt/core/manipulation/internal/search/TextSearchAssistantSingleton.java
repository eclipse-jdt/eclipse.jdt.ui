package org.eclipse.jdt.core.manipulation.internal.search;

public class TextSearchAssistantSingleton {
	private static TextSearchAssistantSingleton instance = new TextSearchAssistantSingleton();
	public static final TextSearchAssistantSingleton getDefault() {
		return instance;
	}

	private ITextSearchRunner runner;
	private TextSearchAssistantSingleton() {

	}

	public void setRunner(ITextSearchRunner runner) {
		this.runner = runner;
	}

	public ITextSearchRunner getRunner() {
		return this.runner;
	}
}
