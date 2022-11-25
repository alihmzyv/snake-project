package com.codenjoy.dojo.snake.client;

/*-
 * #%L
 * Codenjoy - it's a dojo-like platform from developers to developers.
 * %%
 * Copyright (C) 2018 Codenjoy
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.codenjoy.dojo.client.Solver;
import com.codenjoy.dojo.client.WebSocketRunner;
import com.codenjoy.dojo.services.*;
import com.codenjoy.dojo.snake.client.graph.Graph;
import com.codenjoy.dojo.snake.client.graph.Node;
import com.codenjoy.dojo.snake.client.graph.PointHelper;
import com.codenjoy.dojo.snake.model.Elements;

import java.util.List;

import static com.codenjoy.dojo.snake.client.graph.PointHelper.getNeighbours;

/**
 * User: your name
 */
public class YourSolver implements Solver<Board> {

    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://159.89.27.106/codenjoy-contest/board/player/oml71wa8arzdzmwqkgfi?code=4180714991633839453",
                new YourSolver(new RandomDice()),
                new Board());
    }

    private Dice dice;
    private Board board;
    private int boardSize;
    private int dh;
    private int dw;
    private Point apple;
    private Point stone;
    private Point head;
    private Direction currDir;
    private List<Point> snake;
    private List<Point> emptyPoints;
    private List<Point> barriers;
    private Graph<Point> graph;

    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    @Override
    public String get(Board board) {
        this.board = board;
        fetchData();
        return solve();
    }

    private void fetchData() {
        parseBoard();
        constructGraph();
    }

    private String solve() {
        List<Node<Point>> path = graph.DijkstraShortestPath(graph.getNode(Node.of(head)).get(),
                graph.getNode(Node.of(apple)).get());
        return PointHelper.getDir(head, path.get(1).getObj()).toString();
    }

    private void parseBoard() {
        this.boardSize = this.board.size();
        this.dh = 1;
        this.dw = 1;
        this.apple = board.getApples().get(0);
        this.stone = board.getStones().get(0);
        this.head = board.getHead();
        this.currDir = board.getSnakeDirection();
        this.snake = board.getSnake();
        this.emptyPoints = board.get(Elements.NONE);
        this.barriers = board.getBarriers();
    }

    private void constructGraph() {
        this.graph = new Graph<>(true);
        addEmptyPoints();
        addApple();
        addHead();
        addSnake();
        addStone();
    }

    private void addApple() {
        Node<Point> appleNode = Node.of(apple);
        graph.addNode(appleNode);
        graph.getAllNodes()
                .stream()
                .filter(node -> getNeighbours(apple, dw, dh, boardSize, barriers).contains(node.getObj()))
                .forEach(node -> graph.addEdge(node, appleNode, 1));
    }

    private void addStone() {
        graph.addNode(Node.of(stone));
    }

    private void addSnake() {
        graph.addAllNodes(Node.of(snake));
    }

    private void addHead() {
        Node<Point> headNode = Node.of(head);
        getNeighbours(head, dw, dh, boardSize, barriers)
                .stream().map(Node::of)
                .forEach(neighbourNode -> graph.addEdge(headNode, neighbourNode, 1));
    }

    private void addEmptyPoints() {
        graph.addAllNodes(Node.of(emptyPoints));
        graph.getAllNodes()
                .forEach(node -> {
                    graph.getAllNodes()
                            .stream()
                            .filter(pointNode ->
                                    getNeighbours(node.getObj(), dw, dh, boardSize, barriers).contains(pointNode.getObj()))
                            .forEach(pointNode -> graph.addEdge(node, pointNode, 1));
                });
    }

}
