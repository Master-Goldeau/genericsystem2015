package org.genericsystem.todoApp;

import javafx.scene.Node;
import org.genericsystem.todoApp.binding.Binding;
import org.genericsystem.todoApp.binding.BindingContext;

public class ViewContext {
	private final Element template;
	private final Node node;
	private final ModelContext modelContext;
	private final ViewContext parent;
	private boolean initChildren = true;

	public ViewContext(ModelContext modelContext, Element template, Node node, ViewContext parent) {
		this.template = template;
		this.node = node;
		this.modelContext = modelContext;
		this.parent = parent;
	}

	public ViewContext init() {
		BindingContext bindingContext = new BindingContext(modelContext, this);
		for (Binding<?> binding : template.bindings)
			binding.init(bindingContext);
		if (initChildren)
			initChildren();
		return this;
	}

	// unction<Node,List<Node>> function;
	// public List<Node> getChildrenNodes(Node parentNode) {
	// if (parentNode instanceof Pane)
	// return ((Pane) parentNode).getChildren();
	// return Collections.emptyList();
	// }

	// public void removeChildNode(Node parentNode, Node node, Function<Node, List<Node>> function) {
	// function.apply(parentNode).remove(node);
	// }

	// public static Node createChildNode(Node parentNode, Element childElement) {
	// Node childNode = createNode(childElement.classNode);
	// childElement.getGraphicChildren(parentNode).add(childNode);
	// return childNode;
	//
	// }

	public void destroyChild() {
		template.getGraphicChildren(getParent().getNode()).remove(getNode());
	}

	public void bind(ModelContext modelContext) {
		new ViewContext(modelContext, template, node, this).initChildren();
	}

	private static Node createNode(Class<? extends Node> clazz) {
		try {
			return clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private void initChildren() {
		for (Element childElement : template.getChildren()) {
			Node childNode = createNode(childElement.classNode);
			childElement.getGraphicChildren(node).add(childNode);
			ViewContext childViewContext = new ViewContext(modelContext, childElement, childNode, this);
			modelContext.register(childViewContext);
			childViewContext.init();
		}
	}

	public void disableInitChildren() {
		this.initChildren = false;
	}

	public Element getTemplate() {
		return template;
	}

	public Node getNode() {
		return node;
	}

	public ModelContext getModelContext() {
		return modelContext;
	}

	public ViewContext getParent() {
		return parent;
	}
}