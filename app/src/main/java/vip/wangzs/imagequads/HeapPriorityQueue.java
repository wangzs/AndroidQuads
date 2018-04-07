package vip.wangzs.imagequads;

import java.util.Map.Entry;

/**
 * Created by wangzs on 2018/4/3.
 */

public class HeapPriorityQueue<K extends Comparable, V>
        implements PriorityQueue<K, V> {
    private Entry[] storage; // The Heap itself in array form
    private int tail; // Index of last element in the heap

    /**
     * Default constructor
     */
    public HeapPriorityQueue() {
        storage = new Entry[10000];
        tail = 0;
    }

    /**
     * HeapPriorityQueue constructor with max storage of size elements
     */
    public HeapPriorityQueue(int size) {
        storage = new Entry[size];
        tail = 0;
    }

    /****************************************************
     *
     * Priority Queue Methods
     *
     ****************************************************/

    /**
     * Returns the number of items in the priority queue. O(1)
     *
     * @return number of items
     */
    public int size() {
        return tail;
    }

    /**
     * Tests whether the priority queue is empty. O(1)
     *
     * @return true if the priority queue is empty, false otherwise
     */
    public boolean isEmpty() {
        return tail == 0;
    }

    /**
     * Inserts a key-value pair and returns the entry created. O(log(n))
     *
     * @param key   the key of the new entry
     * @param value the associated value of the new entry
     * @return the entry storing the new key-value pair
     * @throws IllegalArgumentException if the heap is full
     */
    public Entry<K, V> insert(K key, V value) throws IllegalArgumentException {
        if (tail >= storage.length) {
            return null;
        } else {
            Entry<K, V> newEntry = new MyEntry<>(key, value);
            storage[tail] = newEntry;
            upHeap(tail++);
            return newEntry;
        }
    }

    /**
     * Returns (but does not remove) an entry with minimal key. O(1)
     *
     * @return entry having a minimal key (or null if empty)
     */
    public Entry<K, V> min() {
        return storage[0];
    }

    /**
     * Removes and returns an entry with minimal key. O(log(n))
     *
     * @return the removed entry (or null if empty)
     */
    public Entry<K, V> removeMin() {
        Entry<K, V> root = storage[0];
        if (size() == 0) {
            return root;
        } else {
            storage[0] = storage[tail - 1];
            storage[tail - 1] = null;
            tail--;
            downHeap(0);
        }
        return root;
    }

    /****************************************************
     *
     * Methods for Heap Operations
     *
     ****************************************************/

    /**
     * Algorithm to place element after insertion at the tail. O(log(n))
     */
    private void upHeap(int location) {
        while (location >= 0) {
            int parent = parent(location);
            if (((Comparable) (storage[parent].getKey())).compareTo((storage[location].getKey())) > 0) {
                swap(location, parent);
                location = parent;
            } else {
                break;
            }
        }
        return;
    }

    /**
     * Algorithm to place element after removal of root and tail element placed
     * at root. O(log(n))
     */
    private void downHeap(int location) {
        int left, right, smaller_child;
        while (hasLeft(location)) {
            left = 2 * location + 1;
            smaller_child = left;
            if (hasRight(location)) {
                right = 2 * location + 2;
                if (((Comparable) (storage[left].getKey())).compareTo(storage[right].getKey()) > 0) {
                    smaller_child = right;
                }
            }
            if (((Comparable) (storage[location].getKey())).compareTo(storage[smaller_child].getKey()) > 0) {
                swap(location, smaller_child);
                location = smaller_child;
            } else {
                break;
            }
        }
        return;
    }

    /**
     * Find parent of a given location, Parent of the root is the root O(1)
     */
    private int parent(int location) {
        int parent = 0;
        if ((location % 2 == 0) && (location != 0)) {
            parent = (location - 2) / 2;
        } else if ((location % 2 == 1) && (location != 0)) {
            parent = (location - 1) / 2;
        }
        return parent;
    }

    /**
     * Inplace swap of 2 elements, assumes locations are in array O(1)
     */
    private void swap(int location1, int location2) {
        Entry<K, V> temp = storage[location2];
        storage[location2] = storage[location1];
        storage[location1] = temp;
        return;
    }

    private boolean hasLeft(int location) {
        if (2 * location + 1 < tail) {
            return true;
        }
        return false;
    }

    private Entry<K, V> getLeft(int loc) {
        if (hasLeft(loc)) {
            return storage[2 * loc + 1];
        }
        return null;
    }

    private boolean hasRight(int location) {
        if (2 * location + 2 < tail) {
            return true;
        }
        return false;
    }

    private Entry<K, V> getRight(int loc) {
        if (hasRight(loc)) {
            return storage[2 * loc + 2];
        }
        return null;
    }

    public void print() {
        for (int i = 0; i < storage.length; i++) {
            if (storage[i] != null) {
                System.out.println(storage[i].getValue());
            } else {
                System.out.println("null");
            }
        }
    }

    public V getValue(int index) {
        if (storage[index] != null) {
            return (V) storage[index].getValue();
        }
        return null;
    }
}

interface PriorityQueue<K extends Comparable, V> {

    /**
     * Returns the number of items in the priority queue.
     *
     * @return number of items
     */
    int size();

    /**
     * Tests whether the priority queue is empty.
     *
     * @return true if the priority queue is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Inserts a key-value pair and returns the entry created.
     *
     * @param key   the key of the new entry
     * @param value the associated value of the new entry
     * @return the entry storing the new key-value pair
     * @throws IllegalArgumentException if the key is unacceptable for this queue
     */
    Entry<K, V> insert(K key, V value) throws IllegalArgumentException;

    /**
     * Returns (but does not remove) an entry with minimal key.
     *
     * @return entry having a minimal key (or null if empty)
     */
    Entry<K, V> min();

    /**
     * Removes and returns an entry with minimal key.
     *
     * @return the removed entry (or null if empty)
     */
    Entry<K, V> removeMin();
}

final class MyEntry<K, V> implements Entry<K, V> {
    private final K key;
    private V value;

    public MyEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V old = this.value;
        this.value = value;
        return old;
    }
}

