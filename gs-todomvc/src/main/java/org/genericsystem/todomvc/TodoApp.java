package org.genericsystem.todomvc;

import org.genericsystem.common.Root;
import org.genericsystem.reactor.ReactorStatics;
import org.genericsystem.reactor.annotations.DependsOnModel;
import org.genericsystem.reactor.appserver.ApplicationServer;
import org.genericsystem.reactor.html.HtmlApp;
import org.genericsystem.reactor.html.HtmlButton;
import org.genericsystem.reactor.html.HtmlCheckBox;
import org.genericsystem.reactor.html.HtmlDiv;
import org.genericsystem.reactor.html.HtmlFooter;
import org.genericsystem.reactor.html.HtmlH1;
import org.genericsystem.reactor.html.HtmlHeader;
import org.genericsystem.reactor.html.HtmlHyperLink;
import org.genericsystem.reactor.html.HtmlInputText;
import org.genericsystem.reactor.html.HtmlLabel;
import org.genericsystem.reactor.html.HtmlLi;
import org.genericsystem.reactor.html.HtmlSection;
import org.genericsystem.reactor.html.HtmlSpan;
import org.genericsystem.reactor.html.HtmlStrong;
import org.genericsystem.reactor.html.HtmlUl;
import org.genericsystem.todomvc.Todos.Completed;

import io.vertx.core.http.ServerWebSocket;

/**
 * @author Nicolas Feybesse
 *
 */
@DependsOnModel({ Todos.class, Completed.class })
public class TodoApp extends HtmlApp<TodoList> {

	public static void main(String[] mainArgs) {
		ApplicationServer.sartSimpleWebApp(mainArgs, TodoApp.class, TodoList.class, "/todo/");
	}

	public TodoApp(Root engine, ServerWebSocket webSocket) {
		super(webSocket);
		new HtmlDiv<TodoList>(this) {
			{
				new HtmlSection<TodoList>(this) {
					{
						addStyleClass("todoapp");
						new HtmlHeader<TodoList>(this) {
							{
								addStyleClass("header");
								new HtmlH1<TodoList>(this).setText("todos");
								new HtmlInputText<TodoList>(this) {
									{
										addStyleClass("new-todo");
										bindAction(TodoList::create);
										bindTextBidirectional(TodoList::getName);
									}
								};
							}
						};
						new HtmlSection<TodoList>(this) {
							{
								addStyleClass("main");

								new HtmlUl<TodoList>(this) {
									{

										addStyleClass("todo-list");

										new HtmlLi<Todo>(this) {
											{
												storeProperty("completed", Todo::getCompleted);
												forEach(TodoList::getFiltered);
												bindOptionalStyleClass("completed", "completed");
												new HtmlDiv<Todo>(this) {
													{
														addStyleClass("view");
														new HtmlCheckBox<Todo>(this) {
															{
																addStyleClass("toggle");
																addPrefixBinding(todo -> {
																	if (Boolean.TRUE.equals(getObservableValue("completed", todo).getValue())) {
																		todo.getObservableAttributes(this).put(ReactorStatics.CHECKED, ReactorStatics.CHECKED);
																	}
																});

																// bindAction((model, value) -> {
																// if (value == null || value.isEmpty()) {
																// model.setCompletion(false);
																// } else {
																// model.setCompletion(true);
																// }
																// });
																bindOptionalBiDirectionalAttribute("completed", ReactorStatics.CHECKED, ReactorStatics.CHECKED);
																bindActionToValueChangeListener("completed", (model, nva) -> model.setCompletion((Boolean) nva));
															}
														};
														new HtmlLabel<Todo>(this) {
															{
																bindText(Todo::getTodoString);
															}
														};
														new HtmlButton<Todo>(this) {
															{
																addStyleClass("destroy");
																bindAction(Todo::remove);
															}
														};
													}
												};

											}
										};
									}
								};
							}
						};

						new HtmlFooter<TodoList>(this) {
							{
								addStyleClass("footer");
								bindOptionalStyleClass("hide", "hasNoTodo", TodoList::getHasNoTodo);
								new HtmlDiv<Todo>(this) {
									{
										new HtmlSpan<TodoList>(this) {
											{
												addStyleClass("todo-count");
												new HtmlStrong<TodoList>(this).bindText(TodoList::getActiveCount);
												new HtmlSpan<TodoList>(this).bindText(TodoList::getItems);
											}
										};
										new HtmlUl<TodoList>(this) {
											{
												addStyleClass("filters");
												new HtmlLi<TodoList>(this) {
													{
														new HtmlHyperLink<TodoList>(this, "All", TodoList::showAll).bindOptionalStyleClass("selected", "allMode", TodoList::getAllMode);
													}
												};
												new HtmlLi<TodoList>(this) {
													{
														new HtmlHyperLink<TodoList>(this, "Actives", TodoList::showActive).bindOptionalStyleClass("selected", "activeMode", TodoList::getActiveMode);
													}
												};
												new HtmlLi<TodoList>(this) {
													{
														new HtmlHyperLink<TodoList>(this, "Completes", TodoList::showCompleted).bindOptionalStyleClass("selected", "completeMode", TodoList::getCompletedMode);
													}
												};
											}
										};
										new HtmlButton<TodoList>(this) {
											{
												addStyleClass("clear-completed");
												bindAction(TodoList::removeCompleted);
												bindText(TodoList::getClearCompleted);
												bindOptionalStyleClass("hide", "hasNoCompleted", TodoList::getHasNoCompleted);
											}
										};

									}
								};
							}
						};
					}
				};
				new HtmlFooter<TodoList>(this) {
					{
						new HtmlDiv<TodoList>(this) {
							{
								addStyleClass("save-cancel");
								new HtmlButton<TodoList>(this) {
									{
										addStyleClass("save");
										bindText(TodoList::getSave);
										bindAction(TodoList::save);
									}
								};
								new HtmlButton<TodoList>(this) {
									{
										addStyleClass("cancel");
										bindText(TodoList::getCancel);
										bindAction(TodoList::cancel);
									}
								};
							}
						};
					}
				};
			}
		};
	}
}