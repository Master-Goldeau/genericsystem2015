package org.genericsystem.distributed.cacheonserver.ui.js.todomvc;

import io.vertx.core.http.ServerWebSocket;

import org.genericsystem.distributed.cacheonserver.ui.js.HtmlNode;
import org.genericsystem.distributed.cacheonserver.ui.js.Model;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlApplication;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlButton;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlCheckBox;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlDiv;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlH1;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlHeader;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlInputText;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlLabel;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlLi;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlSection;
import org.genericsystem.distributed.cacheonserver.ui.js.components.HtmlUl;

public class HtmlAdmin extends HtmlApplication {

	public HtmlAdmin(Model model, HtmlNode parentNode, ServerWebSocket webSocket) {
		super(model, parentNode, webSocket);
	}

	@Override
	protected void initChildren() {

		HtmlDiv div = new HtmlDiv(this);
		{
			HtmlSection todoapp = (HtmlSection) new HtmlSection(div).setStyleClass("todoapp");
			{
				HtmlHeader header = (HtmlHeader) new HtmlHeader(todoapp).setStyleClass("header");
				{
					new HtmlH1(header).addBoot(HtmlNode::getText, "Todos");
					new HtmlInputText(header).setStyleClass("new-todo").addActionBinding(HtmlNode::getActionProperty, TodoList::create).addBidirectionalBinding(HtmlNode::getText, TodoList::getName);
				}
				HtmlSection main = (HtmlSection) new HtmlSection(todoapp).setStyleClass("main");
				{
					HtmlUl todoList = (HtmlUl) new HtmlUl(main).setStyleClass("todo-list");
					{
						HtmlLi li = (HtmlLi) new HtmlLi(todoList).setStyleClass("completed").forEach(TodoList::getFiltered);
						{
							HtmlDiv todoDiv = (HtmlDiv) new HtmlDiv(li).setStyleClass("view");
							{
								new HtmlCheckBox(todoDiv).setStyleClass("toggle");
								new HtmlLabel(todoDiv).addBinding(HtmlNode::getText, Todo::getTodoString);
								new HtmlButton(todoDiv).setStyleClass("destroy").addActionBinding(HtmlNode::getActionProperty, Todo::remove);
							}
						}
					}
				}
			}
		}

		// new HtmlInputText(div).addBidirectionalBinding(HtmlNode::getText, TodoList::getName);
		//
		// new HtmlButton(div).addActionBinding(HtmlNode::getActionProperty, TodoList::create).addBoot(HtmlNode::getText, "Add");
		//
		// HtmlDiv todoList = (HtmlDiv) new HtmlDiv(this).forEach(TodoList::getFiltered);
		// {
		// new HtmlLabel(todoList).addBinding(HtmlNode::getText, Todo::getTodoString);
		// new HtmlButton(todoList).addActionBinding(HtmlNode::getActionProperty, Todo::select).addBoot(HtmlNode::getText, "Select");
		// new HtmlButton(todoList).addActionBinding(HtmlNode::getActionProperty, Todo::remove).addBoot(HtmlNode::getText, "Remove");
		// }
		//
		// HtmlDiv selectionContext = (HtmlDiv) new HtmlDiv(this).select(TodoList::getSelection);
		// {
		// new HtmlLabel(selectionContext).setStyleClass("lab2").setStyleClass("lab").addBinding(HtmlNode::getText, Todo::getTodoString);
		// }
	}
}
