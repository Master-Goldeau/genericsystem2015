package org.genericsystem.distributed.cacheonserver.ui.exemple;

import org.genericsystem.distributed.ui.HtmlElement;
import org.genericsystem.distributed.ui.components.HtmlH1;
import org.genericsystem.distributed.ui.components.HtmlSection;

public class AppHeaderHtml extends HtmlSection<AppModel> {
	public AppHeaderHtml(HtmlElement<?, ?, ?> parent) {
		super(parent);
		addStyleClass("gsheader");
	}

	@Override
	protected void initChildren() {
		new HtmlH1<AppModel>(this).setText("Reactive System Live Demo");
	}
}