package com.codenjoy.dojo.snake.client;

import org.jgrapht.GraphPath;

import java.util.List;

public interface GraphHelper {
    static <A, B> int getPathLength(GraphPath<A, B> path) {
        return path.getLength();
    }
    static <A, B> double getPathWeight(GraphPath<A, B> path) {
        return path.getWeight();
    }

    static <A, B> List<A> getVertexList(GraphPath<A, B> path) {
        return path.getVertexList();
    }
}
