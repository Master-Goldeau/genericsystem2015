package org.genericsystem.gsadmin;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Group;

import org.genericsystem.common.Generic;
import org.genericsystem.distributed.cacheonclient.CocClientEngine;
import org.genericsystem.ui.Element;
import org.genericsystem.ui.components.GSButton;
import org.genericsystem.ui.components.GSHBox;
import org.genericsystem.ui.components.GSTextField;
import org.genericsystem.ui.components.GSVBox;
import org.genericsystem.ui.utils.Transformation;

public class GenericList {

	private CocClientEngine engine;
	private Transformation<TypeWrapper, Generic> genericWrapperList;
	private ObservableList<Generic> dependenciesObservableList;
	private Property<String> name = new SimpleStringProperty();
	private Property<TypeWrapper> selection = new SimpleObjectProperty<>();

	/********************************************************************************/
	public static void init(Element<Group> scene) {

		GSVBox mainPanel = new GSVBox(scene, Group::getChildren).setPrefHeight(600);
		{
			GSHBox creationPanel = new GSHBox(mainPanel);
			{
				new GSTextField(creationPanel).bindTextProperty(GenericList::getName).setPrefWidth(350);
				new GSButton(creationPanel, "Create Todo", GenericList::create).setPrefWidth(200);
			}

			new GSHBox(mainPanel).forEach(GenericList::getGenerics).include(TypeWrapper::init);

			GSHBox commandPanel = new GSHBox(mainPanel).setSpacing(10);
			{
				new GSButton(commandPanel, "Flush").setAction(GenericList::flush);
				new GSButton(commandPanel, "Clear").setAction(GenericList::clear);
				new GSButton(commandPanel, "Mount").setAction(GenericList::mount);
				new GSButton(commandPanel, "Unmount").setAction(GenericList::unmount);
			}

			new GSHBox(mainPanel).select(GenericList::getSelection).include(InstanceWrapper::init);
		}
	}

	/***********************************************************************************/
	public GenericList(CocClientEngine engine) throws InterruptedException {
		this.engine = engine;
		dependenciesObservableList = engine.getCurrentCache().getObservableDependencies(engine);
		genericWrapperList = new Transformation<TypeWrapper, Generic>(dependenciesObservableList, generic -> new TypeWrapper(generic));
	}

	/*********************************************************************************/
	public void create() {
		engine.addInstance(name.getValue());
	}

	public void flush() {
		engine.getCurrentCache().flush();
	}

	public void clear() {
		engine.getCurrentCache().clear();

	}

	public void mount() {
		engine.getCurrentCache().mount();
	}

	public void unmount() {
		engine.getCurrentCache().unmount();
	}

	public void remove(TypeWrapper genWrapper) {
		genWrapper.remove();
	}

	/************************************************************************************/

	public Property getName() {
		return name;
	}

	public ObservableList<TypeWrapper> getGenerics() {
		return genericWrapperList;
	}

	public Property<TypeWrapper> getSelection() {
		return selection;
	}
}
