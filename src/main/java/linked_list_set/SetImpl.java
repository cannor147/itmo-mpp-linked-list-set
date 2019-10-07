package linked_list_set;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class SetImpl implements Set {

    private static class Node {
        private AtomicMarkableReference<Node> next;
        private int x;

        private Node(int x, Node next, boolean removed) {
            this.next = new AtomicMarkableReference<>(next, removed);
            this.x = x;
        }
    }

    private static class Window {
        Node cur;
        Node next;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null, false), false);

    private Window findWindows(int x) {
        while (true) {
            Window window = new Window();
            window.cur = head;
            window.next = window.cur.next.getReference();
            while (window.next.x < x) {
                AtomicMarkableReference<Node> s = window.next.next;
                if (s.isMarked()) {
                    if (!window.cur.next.compareAndSet(window.next, s.getReference(), false, false)) {
                        continue;
                    }
                    window.next = s.getReference();
                } else {
                    window.cur = window.next;
                    window.next = window.cur.next.getReference();
                }
            }
            if (!window.next.next.isMarked()) {
                return window;
            }
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window window = findWindows(x);
            if (window.next.x == x) {
                return false;
            }
            Node node = new Node(x, window.next, false);
            if (window.cur.next.compareAndSet(window.next, node, false, false)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window window = findWindows(x);
            if (window.next.x != x) {
                return false;
            }
            AtomicMarkableReference<Node> s = window.next.next;
            if (window.next.next.compareAndSet(s.getReference(), s.getReference(), false, true)) {
                window.cur.next.compareAndSet(window.next, s.getReference(), false, false);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window window = findWindows(x);
        return window.next.x == x;
    }
}