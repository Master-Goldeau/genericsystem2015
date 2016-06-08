package org.genericsystem.spring;

import java.io.Serializable;

import org.genericsystem.api.core.annotations.SystemGeneric;
import org.genericsystem.common.Generic;
import org.junit.Test;

public class PersistenceTest extends AbstractTest {
	@Test
	public void testCount() {
		Generic count = engine.find(Count.class);
		if (count.getInstances().isEmpty()) {
			count.addInstance(0);
			engine.getCurrentCache().flush();
		}
		Generic actualValue = count.getInstances().first();
		Serializable previousValue = actualValue.getValue();
		actualValue.updateValue((int) actualValue.getValue() + 1);
		log.info("previousValue " + previousValue + " actualValue " + actualValue.getValue());
		engine.getCurrentCache().flush();
		engine.close();
	}

	@SystemGeneric
	public static class Count {
	}

}
