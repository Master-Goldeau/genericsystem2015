package org.genericsystem.reactor.gscomponents3;

import org.genericsystem.reactor.annotations.ReactorDependencies;
import org.genericsystem.reactor.annotations.Styles.Flex;
import org.genericsystem.reactor.annotations.Styles.Overflow;
import org.genericsystem.reactor.annotations.Styles.ReverseFlexDirection;
import org.genericsystem.reactor.gscomponents.GSDiv;
import org.genericsystem.reactor.gscomponents3.Table.ContentRow;

@Flex("1 1 0%")
@Overflow("hidden")
@ReactorDependencies(ContentRow.class)
public class Table extends GSDiv {

	@ReverseFlexDirection
	public static class HeaderRow extends GSComposite {

	}

	@ReverseFlexDirection
	public static class ContentRow extends GSComposite {

	}

	@ReverseFlexDirection
	public static class FooterRow extends GSComposite {

	}
}