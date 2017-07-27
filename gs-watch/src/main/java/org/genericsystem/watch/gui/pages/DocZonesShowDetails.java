package org.genericsystem.watch.gui.pages;

import org.genericsystem.reactor.annotations.BindText;
import org.genericsystem.reactor.annotations.Children;
import org.genericsystem.reactor.annotations.ForEach;
import org.genericsystem.reactor.annotations.Style;
import org.genericsystem.reactor.annotations.Style.FlexDirectionStyle;
import org.genericsystem.reactor.annotations.StyleClass;
import org.genericsystem.reactor.gscomponents.FlexDirection;
import org.genericsystem.reactor.gscomponents.FlexDiv;
import org.genericsystem.reactor.gscomponents.HtmlTag.HtmlHyperLink;
import org.genericsystem.reactor.gscomponents.Modal.ModalWithDisplay;
import org.genericsystem.watch.gui.pages.DocZonesShowDetails.FiltersDiv;
import org.genericsystem.watch.gui.utils.ObservableListExtractorCustom.OCR_SELECTOR;
import org.genericsystem.watch.gui.utils.TextBindingCustom.OCR_TEXT;

//@Children(FlexDiv.class)
@Children(path = FlexDiv.class, value = { HtmlHyperLink.class, FiltersDiv.class })
@StyleClass(path = { FlexDiv.class, FiltersDiv.class }, value = "filter-results")
@Style(path = FlexDiv.class, name = "display", value = "block")
@Style(path = FlexDiv.class, name = "padding", value = "1.5em")
public class DocZonesShowDetails extends ModalWithDisplay {

	@FlexDirectionStyle(FlexDirection.ROW)
	@StyleClass("ocr-row")
	@Children({ FilterNames.class, FiltersOcrText.class })
	@ForEach(OCR_SELECTOR.class)
	public static class FiltersDiv extends FlexDiv {
		// For each filter, create a row with the filtername and the results of the ocr
	}

	@FlexDirectionStyle(FlexDirection.COLUMN)
	@BindText
	@StyleClass({ "ocr", "ocr-label" })
	public static class FilterNames extends FlexDiv {
		// Print the filtername
	}

	@FlexDirectionStyle(FlexDirection.COLUMN)
	@BindText(OCR_TEXT.class)
	@StyleClass({ "ocr", "ocr-text" })
	public static class FiltersOcrText extends FlexDiv {
		// Print the ocr text for the corresponding filter
	}

}