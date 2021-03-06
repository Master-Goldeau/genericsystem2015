package org.genericsystem.defaults.tools;

import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.WeakListener;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class BidirectionalBinding<S, T> implements ChangeListener<Object>, WeakListener {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final WeakReference<Property<S>> stringPropertyRef;
	private final WeakReference<Property<T>> otherPropertyRef;
	private boolean updating;
	private final Function<S, T> fromS;
	private final Function<T, S> toS;

	public static <S, T> BidirectionalBinding<S, T> bind(Property<S> property, Property<T> otherProperty, Function<S, T> to, Function<T, S> from) {
		final BidirectionalBinding<S, T> binding = new BidirectionalBinding<S, T>(property, otherProperty, to, from);
		property.setValue(from.apply(otherProperty.getValue()));
		property.addListener(binding);
		otherProperty.addListener(binding);
		return binding;
	}

	public BidirectionalBinding(Property<S> stringProperty, Property<T> otherProperty, Function<S, T> fromS, Function<T, S> toS) {
		stringPropertyRef = new WeakReference<Property<S>>(stringProperty);
		otherPropertyRef = new WeakReference<Property<T>>(otherProperty);
		cachedHashCode = stringProperty.hashCode() * otherProperty.hashCode();
		this.fromS = fromS;
		this.toS = toS;
	}

	protected S toString(T value) {
		return toS.apply(value);
	};

	protected T fromString(S value) {
		return fromS.apply(value);
	}

	protected Object getProperty1() {
		return stringPropertyRef.get();
	}

	protected Object getProperty2() {
		return otherPropertyRef.get();
	}

	@Override
	public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
		if (!updating) {
			final Property<S> property1 = stringPropertyRef.get();
			final Property<T> property2 = otherPropertyRef.get();
			if ((property1 == null) || (property2 == null)) {
				if (property1 != null) {
					property1.removeListener(this);
				}
				if (property2 != null) {
					property2.removeListener(this);
				}
			} else {
				try {
					updating = true;
					if (property1 == observable) {
						try {
							property2.setValue(fromString(property1.getValue()));
						} catch (Exception e) {
							logger.warn("Exception while parsing String in bidirectional binding.", e);
							property2.setValue(null);
						}
					} else {
						try {
							property1.setValue(toString(property2.getValue()));
						} catch (Exception e) {
							logger.warn("Exception while converting Object to String in bidirectional binding.", e);
							property1.setValue(null);
						}
					}
				} finally {
					updating = false;
				}
			}
		}
	}

	private final int cachedHashCode;

	@Override
	public int hashCode() {
		return cachedHashCode;
	}

	@Override
	public boolean wasGarbageCollected() {
		return (getProperty1() == null) || (getProperty2() == null);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		final Object propertyA1 = getProperty1();
		final Object propertyA2 = getProperty2();
		if ((propertyA1 == null) || (propertyA2 == null)) {
			return false;
		}

		if (obj instanceof BidirectionalBinding) {
			final BidirectionalBinding otherBinding = (BidirectionalBinding) obj;
			final Object propertyB1 = otherBinding.getProperty1();
			final Object propertyB2 = otherBinding.getProperty2();
			if ((propertyB1 == null) || (propertyB2 == null)) {
				return false;
			}

			if (propertyA1 == propertyB1 && propertyA2 == propertyB2) {
				return true;
			}
			if (propertyA1 == propertyB2 && propertyA2 == propertyB1) {
				return true;
			}
		}
		return false;
	}
}