package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private final AtomicRef<Node> head;

    public SetImpl() {
        Node maximumNode = new Node(Integer.MAX_VALUE, null);
        Node minimumNode = new Node(Integer.MIN_VALUE, maximumNode);
        this.head = new AtomicRef<>(minimumNode);
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window window = findWindow(x);
            if (window.next.x == x) {
                return false;
            }
            Node node = new Node(x, window.next);
            if (window.current.next.compareAndSet(window.next, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window window = findWindow(x + 1);
            if (window.current.x != x) {
                return false;
            }
            Node node = new RemovedNode(window.next);
            if (window.current.next.compareAndSet(window.next, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window window = findWindow(x);
        return window.next.x == x;
    }

    private Window findWindow(int x) {
        while (true) {
            Window window = new Window();
            window.current = head.getValue();
            window.next = window.current.next.getValue();

            while (window.next.x < x || window.next.removed()) {
                if (window.current.removed()) {
                    break;
                }
                if (window.next.removed()) {
                    Node node = window.next.next.getValue().next.getValue();
                    if (!window.current.next.compareAndSet(window.next, node)) {
                        window.next = window.current.next.getValue();
                        continue;
                    }
                    window.next = node;
                } else {
                    window.current = window.next;
                    window.next = window.current.next.getValue();
                }
            }

            if (window.current.removed()) {
                continue;
            }
            return window;
        }
    }

    private static class Window {
        private Node current;
        private Node next;
    }

    private static class Node {
        protected AtomicRef<Node> next;
        private int x;

        private Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }

        public boolean removed() {
            return next.getValue() instanceof RemovedNode;
        }
    }

    private static class RemovedNode extends Node {
        private RemovedNode(Node next) {
            super(Integer.MIN_VALUE, next);
        }
    }
}