package org.genericsystem.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.genericsystem.api.core.FiltersBuilder;
import org.genericsystem.api.core.IGeneric;
import org.genericsystem.api.core.IndexFilter;
import org.genericsystem.api.core.Snapshot;
import org.genericsystem.api.tools.Memoizer;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * @author Nicolas Feybesse
 *
 * @param <T>
 */
public class PseudoConcurrentCollection<T extends IGeneric<?>> implements Snapshot<T> {

	private static interface Index<T> {
		public boolean add(T generic);

		public boolean remove(T generic);

		public Iterator<T> iterator();

		public Stream<T> stream();
	}

	final Map<T, T> map = new HashMap<>();

	private final IndexNode indexesTree = new IndexNode(new IndexImpl(new IndexFilter(FiltersBuilder.NO_FILTER), null));

	private class IndexNode {
		private Index<T> index;

		private SoftValueHashMap<IndexFilter, IndexNode> children = new SoftValueHashMap<IndexFilter, IndexNode>() {

			@Override
			public synchronized IndexNode get(Object key) {
				IndexNode result = super.get(key);
				if (result == null) {
					IndexNode newValue = new IndexNode(new IndexImpl((IndexFilter) key, index));
					put((IndexFilter) key, newValue);
					return newValue;
				}
				return result;
			}
		};

		IndexNode(Index<T> index) {
			this.index = index;
		}

		Index<T> getIndex(List<IndexFilter> filters) {
			if (filters.isEmpty())
				return index;
			return children.get(filters.get(0)).getIndex(filters.subList(1, filters.size()));
		}

		public void add(T generic) {
			if (index.add(generic))
				children.values().forEach(childNode -> childNode.add(generic));
			cleanUp();
		}

		public boolean remove(T generic) {
			boolean result = index.remove(generic);
			if (result)
				children.values().forEach(childNode -> childNode.remove(generic));
			cleanUp();
			return result;
		}

		private void cleanUp() {
			List<IndexFilter> removes = new ArrayList<>(children.keySet()).stream().filter(key -> !key.isAlive()).collect(Collectors.toList());
			removes.forEach(key -> children.remove(key));
		}
	}

	private class IndexImpl implements Index<T> {
		private Node<T> head = null;
		private Node<T> tail = null;
		private final IndexFilter filter;

		IndexImpl(IndexFilter filter, Index<T> parent) {
			this.filter = filter;
			if (parent != null)
				parent.stream().forEach(generic -> {
					if (filter.test(generic))
						add(generic);
				});
		}

		@Override
		public boolean add(T element) {
			assert element != null;
			if (filter.test(element)) {
				Node<T> newNode = new Node<>(element);
				if (head == null)
					head = newNode;
				else
					tail.next = newNode;
				tail = newNode;
				map.put(element, element);
				return true;
			}
			return false;
		}

		@Override
		public boolean remove(T element) {
			Iterator<T> iterator = iterator();
			while (iterator.hasNext())
				if (element.equals(iterator.next())) {
					iterator.remove();
					return true;
				}
			return false;
		}

		@Override
		public Iterator<T> iterator() {
			return new InternalIterator();
		}

		@Override
		public Stream<T> stream() {
			return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new InternalIterator(), 0), false);
		}

		public class InternalIterator extends AbstractIterator<Node<T>, T> implements Iterator<T> {

			private Node<T> last;

			@Override
			protected void advance() {
				last = next;
				next = next == null ? head : next.next;
			}

			@Override
			public T project() {
				return next.content;
			}

			@Override
			public void remove() {
				if (next == null)
					throw new IllegalStateException();
				map.remove(next.content);
				if (last == null) {
					head = next.next;
					next = null;
				} else {
					last.next = next.next;
					if (next.next == null)
						tail = last;
				}
			}
		}
	}

	private static class Node<T> {
		private final T content;
		private Node<T> next;

		private Node(T content) {
			this.content = content;
		}
	}

	@Override
	public Iterator<T> iterator() {
		return indexesTree.getIndex(new ArrayList<>()).iterator();
	}

	@Override
	public Stream<T> unfilteredStream() {
		return indexesTree.getIndex(new ArrayList<>()).stream();
	}

	@Override
	public Snapshot<T> filter(List<IndexFilter> filters) {
		return new Snapshot<T>() {

			@Override
			public Stream<T> unfilteredStream() {
				return indexesTree.getIndex(filters).stream();
			}
		};
	}

	public void add(T element) {
		indexesTree.add(element);
		adds.onNext(element);
	}

	public boolean remove(T element) {
		boolean result = indexesTree.remove(element);
		if (result)
			removes.onNext(element);
		return result;
	}

	@Override
	public T get(Object o) {
		return map.get(o);
	}

	private boolean fireInvalidations = true;
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Subject<T> adds = (Subject) PublishSubject.create().toSerialized();
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Subject<T> removes = (Subject) PublishSubject.create().toSerialized();
	private Function<Predicate<T>, Observable<T>> getAddsM = Memoizer.memoize(p -> adds.hide().filter(x -> fireInvalidations && p.test(x)));
	private Function<Predicate<T>, Observable<T>> getRemsM = Memoizer.memoize(p -> removes.hide().filter(x -> fireInvalidations && p.test(x)));

	public Observable<T> getFilteredAdds(Predicate<T> predicate) {
		return getAddsM.apply(predicate);
	}

	public Observable<T> getFilteredRemoves(Predicate<T> predicate) {
		return getRemsM.apply(predicate);
	}

	public void disableInvalidations() {
		fireInvalidations = false;
	}

	public void enableInvalidations() {
		fireInvalidations = true;
	}
}
