package org.genericsystem.gsadmin;

import org.genericsystem.ui.Element;

public interface Builder {
	public void init(Element<?> parent);

	public abstract class ElementBuilder implements Builder {

	}
}
