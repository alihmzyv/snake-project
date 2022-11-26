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
import com.codenjoy.dojo.services.Dice;
import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.RandomDice;
import com.codenjoy.dojo.snake.client.graph.PointHelper;
import com.codenjoy.dojo.snake.model.Elements;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;

import static com.codenjoy.dojo.snake.client.graph.PointHelper.getNeighbours;

/**
 * User: your name
 */
public class YourSolver2 implements Solver<Board> {

    public static void main(String[] args) {
        WebSocketRunner.runClient(
                // paste here board page url from browser after registration
                "http://159.89.27.106/codenjoy-contest/board/player/oml71wa8arzdzmwqkgfi?code=4180714991633839453",
                new YourSolver2(new RandomDice()),
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
    private List<Point> snake;
    private List<Point> emptyPoints;
    private List<Point> barriers;
    private DefaultDirectedGraph<Point, DefaultEdge> graph;

    public YourSolver2(Dice dice) {
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
        Point nextStep;
        DijkstraShortestPath<Point, DefaultEdge> path1 = new DijkstraShortestPath<>(graph, head, apple);
        if (path1.getPathEdgeList() == null) {
            AllDirectedPaths<Point, DefaultEdge> path2 = new AllDirectedPaths<>(graph);
            nextStep = path2.getAllPaths(Set.of(head), new HashSet<>(emptyPoints), true, null)
                    .stream()
                    .max(Comparator.comparingInt(GraphPath::getLength))
                    .orElseThrow(() -> new RuntimeException("No path at all case not taken into account !"))
                    .getVertexList()
                    .get(1);
        }
        else {
            nextStep = graph.getEdgeTarget(path1.getPathEdgeList().get(0));
        }
        return PointHelper.getDir(head, nextStep).toString();
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
//        Edge<Point> x = new Edge<>();
        this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        addEmptyPoints();
        addApple();
        addHead();
        addSnake();
        addStone();
    }

    private void addApple() {
        graph.addVertex(apple);
        getNeighbours(apple, dw, dh, boardSize, barriers)
                .forEach(neighbourPoint ->
                {
                    graph.addVertex(neighbourPoint);
                    graph.addEdge(neighbourPoint, apple);
                });
    }

    private void addStone() {
        graph.addVertex(stone);
    }

    private void addSnake() {
        snake.forEach(point ->
                graph.addVertex(point));
    }

    private void addHead() {
        graph.addVertex(head);
        getNeighbours(head, dw, dh, boardSize, barriers)
                .forEach(neighbourPoint -> {
                    graph.addVertex(neighbourPoint);
                    graph.addEdge(head, neighbourPoint);
                });
    }

    private void addEmptyPoints() {
        emptyPoints.forEach(point -> {
            graph.addVertex(point);
            getNeighbours(point, dw, dh, boardSize, barriers)
                    .forEach(neighbourPoint -> {
                        graph.addVertex(neighbourPoint);
                        graph.addEdge(point, neighbourPoint);
                    });
        });
    }

}
