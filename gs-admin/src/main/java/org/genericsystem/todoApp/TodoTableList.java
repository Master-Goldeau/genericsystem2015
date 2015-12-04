package org.genericsystem.todoApp;

import java.util.Arrays;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.genericsystem.ui.Binding;
import org.genericsystem.ui.Element;

public class TodoTableList {

	private Property<String> name = new SimpleStringProperty();
	private ObservableList<Todo> todos = FXCollections.observableArrayList();
	private ObservableList<Column> columns = FXCollections.observableArrayList(new Column(), new DeleteColumn());
	private ObservableValue<String> createButtonTextProperty = new SimpleStringProperty("Create Todo");
	private ObservableValue<Number> height = new SimpleDoubleProperty(200);

	public Property<String> getName() {
		return name;
	}

	public ObservableList<Todo> getTodos() {
		return todos;
	}

	public ObservableList<Column> getColumns() {
		return columns;
	}

	public ObservableValue<String> getCreateButtonTextProperty() {
		return createButtonTextProperty;
	}

	public ObservableValue<Number> getHeight() {
		return height;
	}

	public void create() {
		Todo todo = new Todo();
		todo.stringProperty.setValue(name.getValue());
		todos.add(todo);
	}

	public void remove(Todo todo) {
		this.todos.remove(todo);
	}

	public static class Todo {
		private Property<String> stringProperty = new SimpleStringProperty();
		private ObservableValue<String> removeButtonTextProperty = Bindings.concat("Remove : ", stringProperty);

		public ObservableValue<String> getObservable() {
			return stringProperty;
		}

		public ObservableValue<String> getRemoveButtonTextProperty() {
			return removeButtonTextProperty;
		}
	}

	public static class Column extends TableColumn<Todo, String> {
		public Column() {
			super("Todos");
			setMinWidth(130);
			setCellValueFactory(features -> new ReadOnlyObjectWrapper<String>(features.getValue().getObservable().getValue()));
		}
	}

	public static class DeleteColumn extends Column {
		public DeleteColumn() {
			setText("Delete");
			setMinWidth(130);
			setCellFactory(column -> new DeleteButtonCell<>());
		}
	}

	public Node init() {

		Element mainVBox = new Element(null, VBox.class, Binding.bindProperty(VBox::prefHeightProperty, TodoTableList::getHeight));
		Element todoCreateHBox = new Element(mainVBox, HBox.class);
		Element todosCreatLabel = new Element(todoCreateHBox, TextField.class, Binding.bindInputText(TextField::textProperty, TodoTableList::getName));
		Element todosCreateButton = new Element(todoCreateHBox, Button.class, Binding.bindProperty(Button::textProperty, TodoTableList::getCreateButtonTextProperty), Binding.bindAction(Button::onActionProperty, TodoTableList::create));
		Element todoTableView = new Element(mainVBox, TableView.class);
		Element todoTableItems = new Element(todoTableView, Todo.class, TableView<Todo>::getItems, Arrays.asList(Binding.forEach(TodoTableList::getTodos)));
		Element columnsTableItems = new Element(todoTableView, Column.class, TableView<Column>::getColumns, Arrays.asList(Binding.forEach(TodoTableList::getColumns)));

		return mainVBox.apply(this).getNode();
	}
}