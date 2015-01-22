package jboot.loader.boot.resolver;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public abstract class QueueVisitor<T> {

	private Queue<T> queue;
	private Set<T> visited;

	public QueueVisitor() {
		queue = new LinkedList<T>();
		visited = new HashSet<T>();		
	}

	public QueueVisitor(T initialItem) {
		this();
		queue.offer(initialItem);
	}

	protected void beforeTraversal() { }

	abstract protected void visit(T item);

	protected void afterTraversal() { }

	public void traverse() {
		beforeTraversal();
		while (queue.size() > 0) {
			T item = queue.poll();
			visit(item);
		}
		afterTraversal();
	}

	/**
	 * Inserts an item in the visiting queue.</br>
	 * Performs a check to see if the item is already visited in which case the item is <strong>not</strong> added to the queue. 
	 * 
	 * @param item the element to insert in the visiting queue
	 * 
	 * @return true if the element is inserted, false otherwise.
	 */
	protected boolean enqueue(T item) {
		if (!visited.contains(item)) {
			visited.add(item); //mark the item as visited as soon as it is added to the queue. Not right before it is visited.
			return queue.offer(item);
		}
		return false;
	}

	public void clearVisitedItems() {
		visited.clear();
	}

	public void clearVisitingQueue() { //can be used to stop the traversal.
		queue.clear();
	}
}
