package org.genericsystem.reactor.model;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;

import org.genericsystem.common.Generic;
import org.genericsystem.reactor.Model;

/**
 * @author Nicolas Feybesse
 *
 * @param <M>
 */
public class GenericModel extends Model {

	protected static Logger log = LoggerFactory.getLogger(GenericModel.class);
	private final Generic[] generics;
	private final StringExtractor stringExtractor;

	public GenericModel(Generic[] generics, StringExtractor stringExtractor) {
		assert stringExtractor != null;
		this.generics = generics;
		this.stringExtractor = stringExtractor;
	}

	public GenericModel(Model parent, Generic[] generics, StringExtractor stringExtractor) {
		this(generics, stringExtractor);
		this.parent = parent;
	}

	public Generic[] getGenerics() {
		return generics;
	}

	public static Generic[] addToGenerics(Generic generic, Generic[] generics) {
		Generic[] result = new Generic[generics.length + 1];
		result[0] = generic;
		System.arraycopy(generics, 0, result, 1, generics.length);
		return result;
	}

	public Generic getGeneric() {
		return generics[0];
	}

	// TODO KK no cache ?
	public ObservableValue<String> getString() {
		return new ReadOnlyStringWrapper(stringExtractor.apply(getGenerics()[0]));
	}

	public StringExtractor getStringExtractor() {
		return stringExtractor;
	}

	public void remove() {
		getGeneric().remove();
	}

	public void flush() {
		getGeneric().getCurrentCache().flush();
	}

	public void cancel() {
		getGeneric().getCurrentCache().clear();
	}
}
