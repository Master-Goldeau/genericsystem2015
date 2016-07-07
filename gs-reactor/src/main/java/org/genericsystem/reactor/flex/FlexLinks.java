package org.genericsystem.reactor.flex;

import org.genericsystem.reactor.Tag;
import org.genericsystem.reactor.annotation.InstanceColorize;
import org.genericsystem.reactor.html.HtmlLabel;
import org.genericsystem.reactor.model.GenericModel;
import org.genericsystem.reactor.model.GenericModel.StringExtractor;
import org.genericsystem.reactor.model.InputGenericModel;
import org.genericsystem.reactor.model.ObservableListExtractor;

public class FlexLinks {

	public static class FlexLinkDisplayer<M extends GenericModel> extends FlexSection<M> {

		public FlexLinkDisplayer(Tag<?> parent, FlexDirection direction) {
			super(parent, direction);
			content();
		}

		private void content() {
			new FlexSection<GenericModel>(this, this.getReverseDirection()) {
				{
					style(this);
					select_(gs -> gs[0].getComponents().size() < 2 ? gs[0] : null);
					new HtmlLabel<GenericModel>(this).bindText(GenericModel::getString);
				}
			};
			new FlexSection<GenericModel>(this, this.getReverseDirection()) {
				{
					style(this);
					forEach(StringExtractor.SIMPLE_CLASS_EXTRACTOR,
							gs -> ObservableListExtractor.COMPONENTS.apply(gs).filtered(g -> !g.equals(gs[2])));
					new HtmlLabel<GenericModel>(this).bindText(GenericModel::getString);
				}
			};
		}

		private void style(Tag<?> tag) {
			tag.addStyle("overflow", "hidden");
			tag.addStyle("flex", "1");
			tag.addStyle("margin-right", "1px");
			tag.addStyle("margin-bottom", "1px");
			tag.addPrefixBinding(modelContext -> modelContext.getObservableStyles(tag).put("background-color",
					((GenericModel) modelContext).getGeneric().getMeta().getAnnotation(InstanceColorize.class) != null
					? ((GenericModel) modelContext).getString().getValue() : "#dda5e2"));
		}
	}

	public static class FlexLinkTitleDisplayer<M extends GenericModel> extends FlexSection<M> {

		private final ObservableListExtractor observableListExtractor;

		public FlexLinkTitleDisplayer(Tag<?> parent, ObservableListExtractor observableListExtractor, FlexDirection flexDirection) {
			super(parent, flexDirection);
			this.observableListExtractor = observableListExtractor;
			content();
		}

		private void content() {
			new FlexSection<InputGenericModel>(this, this.getDirection()) {
				{
					style(this);
					select_(gs -> gs[0].getComponents().size() < 2 ? gs[0] : null);
					new HtmlLabel<GenericModel>(this) {
						{
							bindText(GenericModel::getString);
						}
					};
				}
			};
			new FlexSection<GenericModel>(this, this.getDirection()) {
				{
					style(this);
					forEach(StringExtractor.SIMPLE_CLASS_EXTRACTOR, observableListExtractor);
					new HtmlLabel<GenericModel>(this) {
						{
							bindText(GenericModel::getString);
						}
					};
				}
			};
		}

		private void style(Tag<?> tag) {
			tag.addStyle("flex", "1");
			tag.addStyle("color", "#ffffff");
			tag.addStyle("background-color", "#ffa5a5");
			tag.addStyle("margin-right", "1px");
			tag.addStyle("margin-bottom", "1px");
			tag.addStyle("justify-content", "center");
		}
	}
}