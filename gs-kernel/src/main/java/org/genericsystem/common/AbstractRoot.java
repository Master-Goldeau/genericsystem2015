package org.genericsystem.common;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import org.genericsystem.api.core.ApiStatics;
import org.genericsystem.api.core.ISignature;
import org.genericsystem.api.core.annotations.InstanceClass;
import org.genericsystem.defaults.DefaultConfig.MetaAttribute;
import org.genericsystem.defaults.DefaultConfig.MetaRelation;
import org.genericsystem.defaults.DefaultConfig.Sequence;
import org.genericsystem.defaults.DefaultConfig.SystemMap;
import org.genericsystem.defaults.DefaultRoot;
import org.genericsystem.defaults.DefaultVertex;
import org.genericsystem.kernel.Statics;

public abstract class AbstractRoot<T extends DefaultVertex<T>> implements DefaultRoot<T>, TProxy<T>, ProxyObject {

	protected final Map<Long, T> tMap = new ConcurrentHashMap<>();
	private final TsGenerator generator = new TsGenerator();
	protected Wrapper<T> contextWrapper = buildContextWrapper();
	private final SystemCache<T> systemCache;
	protected boolean isInitialized = false;

	public AbstractRoot(Class<?>... userClasses) {
		this(Statics.ENGINE_VALUE, userClasses);
	}

	public AbstractRoot(String persistentDirectoryPath, Class<?>... userClasses) {
		this(Statics.ENGINE_VALUE, persistentDirectoryPath, userClasses);
	}

	@Override
	public AbstractRoot<T> getRoot() {
		return this;
	}

	@SuppressWarnings("unchecked")
	public AbstractRoot(Serializable value, String persistentDirectoryPath, Class<?>... userClasses) {
		init((T) this, buildHandler((T) this, Collections.emptyList(), value, Collections.emptyList(), ApiStatics.TS_SYSTEM, ApiStatics.SYSTEM_TS));
		initSubRoot(value, persistentDirectoryPath, userClasses);
		newCache().start();
		systemCache = new SystemCache<>(this, getClass());
		systemCache.mount(Arrays.asList(MetaAttribute.class, MetaRelation.class, SystemMap.class, Sequence.class), userClasses);
		getCurrentCache().flush();
		// shiftContext();
	}

	private MethodHandler handler;

	@Override
	public void setHandler(MethodHandler handler) {
		this.handler = handler;
	}

	@Override
	public MethodHandler getHandler() {
		return handler;
	}

	protected void initSubRoot(Serializable value, String persistentDirectoryPath, Class<?>... userClasses) {
	};

	@Override
	public abstract AbstractContext<T> newCache();

	public static interface Wrapper<T extends DefaultVertex<T>> {
		AbstractContext<T> get();

		void set(AbstractContext<T> context);
	}

	public class ContextWrapper implements Wrapper<T> {

		private AbstractContext<T> context;

		@Override
		public AbstractContext<T> get() {
			return context;
		}

		@Override
		public void set(AbstractContext<T> context) {
			this.context = context;

		}
	}

	protected Wrapper<T> buildContextWrapper() {
		return new ContextWrapper();
	}

	public long pickNewTs() {
		return generator.pickNewTs();
	}

	@Override
	public AbstractContext<T> getCurrentCache() {
		AbstractContext<T> context = contextWrapper.get();
		if (context == null)
			throw new IllegalStateException("Unable to find the current cache. Did you miss to call start() method on it ?");
		return context;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Custom extends T> Custom find(Class<?> clazz) {
		return (Custom) systemCache.find(clazz);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <Custom extends T> Custom bind(Class<?> clazz) {
		return (Custom) systemCache.bind(clazz);
	}

	@Override
	public Class<?> findAnnotedClass(T vertex) {
		return systemCache.getClassByVertex(vertex);
	}

	public static class TsGenerator {
		private final long startTime = System.currentTimeMillis() * Statics.MILLI_TO_NANOSECONDS - System.nanoTime();
		private final AtomicLong lastTime = new AtomicLong(0L);

		public long pickNewTs() {
			long nanoTs;
			long current;
			for (;;) {
				nanoTs = startTime + System.nanoTime();
				current = lastTime.get();
				if (nanoTs - current > 0)
					if (lastTime.compareAndSet(current, nanoTs))
						return nanoTs;
			}
		}
	}

	public T getGenericById(long ts) {
		return tMap.get(ts);
	}

	protected T init(Class<?> clazz, Vertex vertex) {
		T generic = newT(clazz, tMap.get(vertex.getMeta()));
		Wrapped handler = buildHandler(vertex.getMeta() == vertex.getTs() ? generic : getGenericById(vertex.getMeta()), vertex.getSupers().stream().map(this::getGenericById).collect(Collectors.toList()), vertex.getValue(), vertex.getComponents().stream()
				.map(this::getGenericById).collect(Collectors.toList()), vertex.getTs(), vertex.getOtherTs());
		return init(generic, handler);
	}

	T init(Long ts, Class<?> clazz, T meta, List<T> supers, Serializable value, List<T> components, long[] otherTs) {
		T generic = newT(clazz, meta);
		return init(generic, buildHandler(meta != null ? meta : generic, supers, value, components, ts == null ? pickNewTs() : ts, otherTs));
	}

	private T init(T generic, Wrapped handler) {
		((ProxyObject) generic).setHandler(handler);
		assert ((ProxyObject) generic).getHandler() instanceof AbstractRoot.Wrapped;
		T gresult = tMap.putIfAbsent(handler.getTs(), generic);
		assert gresult == null;
		return generic;
	}

	protected abstract Wrapped buildHandler(T meta, List<T> supers, Serializable value, List<T> components, long ts, long[] otherTs);

	protected abstract Class<T> getTClass();

	protected T newT(Class<?> clazz, T meta) {
		InstanceClass metaAnnotation = meta == null ? null : getAnnotedClass(meta).getAnnotation(InstanceClass.class);
		if (metaAnnotation != null)
			if (clazz == null || clazz.isAssignableFrom(metaAnnotation.value()))
				clazz = metaAnnotation.value();
			else if (!metaAnnotation.value().isAssignableFrom(clazz))
				getCurrentCache().discardWithException(new InstantiationException(clazz + " must extends " + metaAnnotation.value()));
		try {
			if (clazz == null || !getTClass().isAssignableFrom(clazz))
				return newInstance(getTClass());
			return newInstance(clazz);
		} catch (IllegalArgumentException e) {
			getCurrentCache().discardWithException(e);
		}
		return null; // Not reached
	}

	private final static ProxyFactory PROXY_FACTORY = new ProxyFactory();
	private final static MethodFilter METHOD_FILTER = method -> method.getName().equals("toString");

	@SuppressWarnings("unchecked")
	private T newInstance(Class<?> clazz) {
		PROXY_FACTORY.setSuperclass(clazz.isInterface() ? Object.class : clazz);
		PROXY_FACTORY.setInterfaces(clazz.isInterface() ? getTClass().isAssignableFrom(clazz) ? new Class[] { clazz } : new Class[] { clazz, getTClass() } : getTClass().isAssignableFrom(clazz) ? new Class[] {} : new Class[] { getTClass() });
		try {
			return (T) PROXY_FACTORY.createClass(METHOD_FILTER).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public Class<?> getAnnotedClass(T vertex) {
		if (vertex.isSystem()) {
			Class<?> annotedClass = findAnnotedClass(vertex);
			if (annotedClass != null)
				return annotedClass;
		}
		return vertex.getClass();
	}

	public boolean isInitialized() {
		return isInitialized;
	}

	public abstract class Wrapped implements MethodHandler, ISignature<T> {

		private final T meta;
		private final List<T> supers;
		private final Serializable value;
		private final List<T> components;
		private final long ts;
		private final long[] otherTs;

		protected Wrapped(T meta, List<T> supers, Serializable value, List<T> components, long ts, long[] otherTs) {
			assert meta != null;
			this.meta = meta;
			this.supers = supers;
			this.value = value;
			this.components = components;
			this.ts = ts;
			this.otherTs = otherTs.clone();
		}

		@Override
		public Object invoke(Object self, Method m, Method proceed, Object[] args) throws Throwable {
			return ((DefaultVertex<?>) self).defaultToString();
		}

		abstract protected AbstractRoot<T> getRoot();

		public Vertex getVertex() {
			return new Vertex(getTs(), getMeta().getTs(), getSupers().stream().map(T::getTs).collect(Collectors.toList()), getValue(), getComponents().stream().map(T::getTs).collect(Collectors.toList()), getOtherTs());
		}

		@Override
		public T getMeta() {
			return meta;
		}

		@Override
		public List<T> getSupers() {
			return supers;
		}

		@Override
		public Serializable getValue() {
			return value;
		}

		@Override
		public List<T> getComponents() {
			return components;
		}

		@Override
		public long getTs() {
			return ts;
		}

		@Override
		public long[] getOtherTs() {
			return otherTs;
		}
	};

	protected AbstractContext<T> start(AbstractContext<T> context) {
		contextWrapper.set(context);
		return context;
	}

	protected void stop(AbstractContext<T> context) {
		assert contextWrapper.get() == context;
		contextWrapper.set(null);
	}

	@Override
	public String toString() {
		return defaultToString();
	}

}
