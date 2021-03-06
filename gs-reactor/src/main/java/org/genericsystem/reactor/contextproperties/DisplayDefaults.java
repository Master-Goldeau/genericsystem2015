package org.genericsystem.reactor.contextproperties;

import org.genericsystem.reactor.Context;

import javafx.beans.property.Property;

public interface DisplayDefaults extends ContextProperty {

	public static final String DISPLAY = "display";

	default void createInitializedDisplayProperty(String initialValue) {
		createNewInitializedProperty(DISPLAY, model -> initialValue);
	}

	default Property<String> getDisplayProperty(Context model) {
		return getContextProperty(DISPLAY, model);
	}
}
