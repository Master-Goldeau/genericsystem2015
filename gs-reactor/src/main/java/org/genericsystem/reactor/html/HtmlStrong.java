package org.genericsystem.reactor.html;

import org.genericsystem.reactor.HtmlElement;
import org.genericsystem.reactor.Model;
import org.genericsystem.reactor.HtmlElement.HtmlDomNode;

/**
 * @author Nicolas Feybesse
 *
 */
public class HtmlStrong<M extends Model> extends HtmlElement<M, HtmlStrong<M>, HtmlDomNode> {

	public HtmlStrong(HtmlElement<?, ?, ?> parent) {
		super(parent, HtmlDomNode.class);
	}

	@Override
	protected HtmlDomNode createNode(Object parent) {
		return new HtmlDomNode("strong");
	}
}