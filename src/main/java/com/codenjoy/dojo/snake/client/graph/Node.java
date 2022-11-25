package com.codenjoy.dojo.snake.client.graph;

import com.codenjoy.dojo.services.Point;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Node<A> {
    // The int n and String name are just arbitrary attributes
    // we've chosen for our nodes these attributes can of course
    // be whatever you need
    A obj;

    private boolean visited;
    LinkedList<Edge<A>> edges;

    public Node(A obj) {
        this.obj = obj;
        visited = false;
        edges = new LinkedList<>();
    }

    public static List<Node<Point>> of (List<Point> points) {
        return points.stream().map(Node::new).collect(Collectors.toCollection(ArrayList::new));
    }

    public static Node<Point> of (Point point) {
        return new Node<>(point);
    }

    public A getObj() {
        return obj;
    }

    boolean isVisited() {
        return visited;
    }

    void visit() {
        visited = true;
    }

    void unvisit() {
        visited = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node<?> that = (Node<?>) o;
        return Objects.equals(obj, that.obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj);
    }

    @Override
    public String toString() {
        return "Node{" +
                "obj=" + obj +
                '}';
    }
}
