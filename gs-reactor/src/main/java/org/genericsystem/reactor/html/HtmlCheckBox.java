package org.genericsystem.reactor.html;

import java.util.function.Function;

import javafx.beans.property.Property;

import org.genericsystem.reactor.HtmlElement;
import org.genericsystem.reactor.HtmlElement.CheckBoxHtmlDomNode;
import org.genericsystem.reactor.Model;

/**
 * @author Nicolas Feybesse
 *
 */
public class HtmlCheckBox<M extends Model> extends HtmlElement<M, CheckBoxHtmlDomNode> {

	public HtmlCheckBox(HtmlElement<?, ?> parent) {
		super(parent, CheckBoxHtmlDomNode.class);
	}

	@Override
	protected CheckBoxHtmlDomNode createNode(Object parent) {
		return new CheckBoxHtmlDomNode();
	}

	public HtmlCheckBox<M> bindCheckedBidirectional(Function<M, Property<Boolean>> applyOnModel) {
		addBidirectionalBinding(CheckBoxHtmlDomNode::getChecked, applyOnModel);
		return this;
	}

}
