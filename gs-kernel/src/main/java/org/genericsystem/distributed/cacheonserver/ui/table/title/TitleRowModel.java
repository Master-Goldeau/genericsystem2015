package org.genericsystem.distributed.cacheonserver.ui.table.title;

import java.io.Serializable;
import java.util.Objects;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import org.genericsystem.distributed.ui.models.GenericCompositeModel;
import org.genericsystem.distributed.ui.models.GenericModel;

public class TitleRowModel extends GenericCompositeModel<GenericModel> {

	public TitleRowModel(CompositeConf<GenericModel> conf) {
		super(conf);
	}

	public ObservableValue<String> getFirstCellString() {
		Serializable value = getGeneric().getValue();
		return new ReadOnlyObjectWrapper<>((value instanceof Class ? ((Class<?>) value).getSimpleName() : Objects.toString(value)) + "(s)");
	}

}