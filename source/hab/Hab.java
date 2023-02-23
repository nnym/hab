package hab;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Hab<A, B> extends AbstractCollection<Hab.Node<A, B>> {
	protected final int initialCapacity;
	protected final float loadFactor;
	protected Node<A, B>[] nodes;
	protected int size;

	public Hab(int capacity, float loadFactor) {
		if (capacity < 0) throw new IllegalArgumentException("capacity = " + capacity + " < 0");
		if (loadFactor <= 0 || loadFactor > 1) throw new IllegalArgumentException("loadFactor = " + loadFactor + " ∉ (0, 1]");

		this.initialCapacity = capacity = superbit(capacity);
		this.loadFactor = loadFactor;
		this.nodes = new Node[capacity];
	}

	public Hab(int capacity) {
		this(capacity, 0.75F);
	}

	public Hab() {
		this(4, 0.75F);
	}

	protected static int superbit(int n) {
		var msb = Integer.highestOneBit(n);
		return Math.max(1, n == msb ? msb : msb << 1);
	}

	public B value(A a) {
		var node = this.node(a);
		return node == null ? null : node.b;
	}

	public Node<A, B> node(A a) {
		for (var capacity = this.nodes.length; capacity >= this.initialCapacity; capacity >>= 1) {
			for (var node = this.collide(a, capacity); node != null; node = node.next) {
				if (this.equals(a, node.a)) {
					return node;
				}
			}
		}

		return null;
	}

	public boolean containsKey(A a) {
		return this.node(a) != null;
	}

	public boolean contains(A a, B b) {
		var node = this.node(a);
		return node != null && Objects.equals(b, node.b);
	}

	public void map(Hab<A, B> hab) {
		hab.forEachPair(this::map);
	}

	public void map(Map<A, B> map) {
		map.forEach(this::map);
	}

	public Node<A, B> map(Node<A, B> node) {
		return this.map(node.a, node.b);
	}

	public Node<A, B> map(A a, B b) {
		return this.map(a, b, true);
	}

	public Node<A, B> unmap(A a) {
		return this.unmap(a, null);
	}

	public Node<A, B> unmap(A a, B b) {
		return this.unmap(a, b, this.nodes.length);
	}

	public Node<A, B> remap(A a, B b) {
		return this.map(a, b, false);
	}

	public Stream<A> keys() {
		return this.stream().map(node -> node.a);
	}

	public Stream<B> values() {
		return this.stream().map(node -> node.b);
	}

	public String toChoppedString() {
		return switch (this.size) {
			case 0 -> "{}";
			case 1 -> "{" + this.stream().findAny().get() + "}";
			default -> this.stream().map(Node::toString).map(entry -> entry.indent(4)).collect(Collectors.joining("", "{\n", "}"));
		};
	}

	public void forEachPair(BiConsumer<? super A, ? super B> action) {
		for (var node : this) {
			action.accept(node.a, node.b);
		}
	}

	@Override public int size() {
		return this.size;
	}

	@Override public boolean addAll(Collection<? extends Node<A, B>> c) {
		c.forEach(this::map);
		return true;
	}

	@Override public boolean add(Node<A, B> node) {
		this.map(node);
		return true;
	}

	@Override public boolean remove(Object o) {
		return o instanceof Node<?, ?> node && this.unmap((A) node.a, (B) node.b) != null;
	}

	@Override public void clear() {
		Arrays.fill(this.nodes, null);
		this.size = 0;
	}

	@Override public Stream<Node<A, B>> stream() {
		return Stream.of(this.toArray());
	}

	@Override public Node<A, B>[] toArray() {
		var array = new Node[this.size];
		var index = 0;

		for (var iterator = this.iterator(); iterator.hasNext(); ++index) {
			if (index >= array.length) {
				array = Arrays.copyOf(array, array.length << 1);
			}

			array[index] = iterator.next();
		}

		return index == array.length ? array : Arrays.copyOf(array, index);
	}

	@Override public Iterator<Node<A, B>> iterator() {
		return new Iterator<>() {
			int index = -1;
			Node<A, B> node;
			boolean computed;

			@Override public boolean hasNext() {
				if (this.computed) {
					return this.node != null;
				}

				this.computed = true;

				if (this.node != null && null != (this.node = this.node.next)) {
					return true;
				}

				while (this.node == null) {
					if (++this.index >= Hab.this.nodes.length) {
						return false;
					}

					if (null != (this.node = Hab.this.nodes[this.index])) {
						return true;
					}
				}

				return false;
			}

			@Override public Node<A, B> next() {
				if (this.hasNext()) {
					this.computed = false;
					return this.node;
				}

				throw new NoSuchElementException();
			}
		};
	}

	@Override public int hashCode() {
		return Objects.hash(this.toArray());
	}

	@Override public boolean equals(Object o) {
		return o instanceof Hab that && this.size == that.size && this.stream().allMatch(node -> that.contains(node.a, node.b));
	}

	@Override public String toString() {
		return switch (this.size) {
			case 0 -> "{}";
			case 1 -> "{" + this.stream().findAny().get() + "}";
			default -> this.stream().map(Node::toString).collect(Collectors.joining(", ", "{", "}"));
		};
	}

	protected Node<A, B> collide(Object a, int capacity) {
		return this.nodes[this.index(a, capacity)];
	}

	protected int index(Object a, int capacity) {
		return Math.max(0, this.hash(a) % capacity);
	}

	protected boolean equals(A a, A a1) {
		return Objects.equals(a, a1);
	}

	protected void grow(int size) {
		if (size >= this.nodes.length * this.loadFactor) {
			this.nodes = Arrays.copyOf(this.nodes, superbit((int) Math.ceil(size / this.loadFactor)));
		}
	}

	protected int hash(Object a) {
		return Objects.hashCode(a);
	}

	protected Node<A, B> map(A a, B b, boolean add) {
		var index = this.index(a, this.nodes.length);
		var previous = this.nodes[index];
		var node = Node.of(a, b);

		if (previous != null && this.equals(a, previous.a)) {
			this.nodes[index] = node;
			node.next = previous.next;

			return previous;
		}

		var removed = this.unmap(a, null, this.nodes.length >>> 1);

		if (add || removed != null) {
			this.grow(this.size++);

			if (previous == null) {
				this.nodes[index] = node;
			} else {
				for (; previous.next != null; previous = previous.next) {}
				previous.next = node;
			}
		}

		return removed;
	}

	private Node<A, B> unmap(A a, B b, int capacity) {
		for (; capacity >= this.initialCapacity; capacity >>>= 1) {
			var index = this.index(a, capacity);
			var node = this.nodes[index];

			if (node != null) {
				if (this.equals(a, node.a) && (b == null || Objects.equals(b, node.b))) {
					this.nodes[index] = node.next;
				} else L: {
					var previous = node;

					for (node = node.next; node != null; node = node.next) {
						if (this.equals(a, node.a) && (b == null || Objects.equals(b, node.b))) {
							previous.next = node.next;
							break L;
						}
					}

					continue;
				}

				--this.size;
				return node;
			}
		}

		return null;
	}

	public static class Node<A, B> {
		public final A a;
		public final B b;
		protected Node<A, B> next;

		public Node(A a, B b) {
			this.a = a;
			this.b = b;
		}

		public static <A, B> Node<A, B> of(A a, B b) {
			return new Node<>(a, b);
		}

		@Override public int hashCode() {
			return Objects.hash(this.a, this.b);
		}

		@Override public boolean equals(Object o) {
			return o instanceof Node<?, ?> that && Objects.equals(this.a, that.a) && Objects.equals(this.b, that.b);
		}

		@Override public String toString() {
			return this.a + " -> " + this.b;
		}
	}
}
