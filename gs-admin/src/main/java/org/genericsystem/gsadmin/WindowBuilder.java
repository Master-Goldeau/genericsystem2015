package org.genericsystem.gsadmin;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;

import org.genericsystem.common.Generic;
import org.genericsystem.distributed.cacheonclient.CocClientEngine;
import org.genericsystem.gsadmin.TableBuilder.TableCellTableBuilder;
import org.genericsystem.gsadmin.TableBuilderModel.TableCellTableModel;
import org.genericsystem.gsadmin.TableBuilderModel.TextTableModel;
import org.genericsystem.ui.Element;
import org.genericsystem.ui.components.GSVBox;

import javafx.beans.property.ReadOnlyStringWrapper;

public class WindowBuilder implements Builder {

	@Override
	public void init(Element<?> parent) {
		GSVBox table = new GSVBox(parent).select(Window::getTable);
		{
			new TableCellTableBuilder<>().init(table);
		}
		
		GSVBox table2 = new GSVBox(parent).select(Window::getSubTable);
		{
			new TableCellTableBuilder<>().init(table2);
		}
	}

	public Window build(ObservableValue<? extends Number> width, ObservableValue<? extends Number> height,CocClientEngine  c) {
		TableCellTableModel<Integer, Integer> tableModel = new TableCellTableModel<>(FXCollections.observableArrayList(0, 1, 2, 3), FXCollections.observableArrayList(0, 1, 2),
				item->col->{
			TextTableModel<Integer, Integer> textTableModel = new TextTableModel<>(FXCollections.observableArrayList(5,8,9,6), FXCollections.observableArrayList(1,1,1,1), 
					item2 -> column -> new ReadOnlyStringWrapper("Cell : " + item2 + " " + column),null,null);
			Table tab = textTableModel.createTable();
			return new ReadOnlyObjectWrapper<Table>(tab);
		},col->new ReadOnlyStringWrapper("col :"+col),item->new ReadOnlyStringWrapper("item :"+item));
		Table table = tableModel.createTable();
		table.getColumnWidth().setValue(300);
		table.getRowHeight().setValue(100);
		table.getFirstRowHeight().setValue(50);
		Window win = new Window(new ReadOnlyObjectWrapper<Table>(table), width, height);
		return win;
	}
	
	public Window buildWithGeneric(ObservableValue<? extends Number> width, ObservableValue<? extends Number> height,CocClientEngine engine) {
		
		TableCellTableModel<Generic, Generic> tableModel = new TableCellTableModel<>( FXCollections.observableArrayList(engine.getSubInstances().toList()),FXCollections.observableArrayList(engine.getAttributes().toList()),
				itemTableCell -> columnTableCell -> {
					TextTableModel<Generic, Generic> textTableModel = new TextTableModel<>(FXCollections.observableArrayList(itemTableCell.getHolders(columnTableCell).toList()), FXCollections.observableArrayList(), 
							null,null,column -> new ReadOnlyStringWrapper("" + column));
					Table tab = textTableModel.createTable();
					return new ReadOnlyObjectWrapper<Table>(tab);
				},	column -> new ReadOnlyStringWrapper("" + column),
					firstColumString->new ReadOnlyStringWrapper(""+firstColumString));
		
		Table table = tableModel.createTable();
		table.getColumnWidth().setValue(300);
		table.getRowHeight().setValue(100);
		table.getFirstRowHeight().setValue(50);
		Window win = new Window(new ReadOnlyObjectWrapper<Table>(table), width, height);
		return win;
	}
}
