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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static com.codenjoy.dojo.snake.client.graph.PointHelper.getNeighbours;

/**
 * User: your name
 */
public class YourSolver implements Solver<Board> {

    public static void main(String[] args) {
        logger.info(String.format("RUN/TRY DATE/TIME: %s", LocalDateTime.now()));
        try {
            WebSocketRunner.runClient(
                    // paste here board page url from browser after registration
                    "http://159.89.27.106/codenjoy-contest/board/player/oml71wa8arzdzmwqkgfi?code=4180714991633839453",
                    new YourSolver(new RandomDice()),
                    new Board());
        }
        catch (Exception exc) {
            logger.info(exc.getStackTrace());
        }
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
    private AllDirectedPaths<Point, DefaultEdge> allPaths;
    private final Random rnd = new Random();
    private static final Logger logger = LogManager.getLogger("SnakeSolver");

    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    @Override
    public String get(Board board) {
        logger.info("Inside get(Board board)");
        this.board = board;
        long start = Instant.now().toEpochMilli();
        try {
            fetchData();
            return solve();
        }
        catch (Exception exc) {
            logger.error(exc.getMessage());
            logger.info("Exception thrown in get(). RIGHT Dir. returned.");
            logger.info(String.format("Time it took: %8.5f seconds", (Instant.now().toEpochMilli() - start) / 1000.0));
            logger.info("Leaving get(Board board)");
            return Direction.RIGHT.toString();
        }
    }

    private void fetchData() {
        parseBoard();
        constructGraph();
    }

    private void parseBoard() {
        logger.info(String.format("The board:\n%s\n", board));
        this.boardSize = board.size();
        this.dh = 1;
        this.dw = 1;
        this.apple = board.getApples().get(0);
        this.stone = board.getStones().get(0);
        this.head = board.getHead();
        this.snake = board.getSnake();
        this.emptyPoints = board.get(Elements.NONE);
        this.barriers = board.getBarriers();
    }

    private void constructGraph() {
        this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        this.allPaths = new AllDirectedPaths<>(graph);
        addEmptyPoints();
        addApple();
        addHead();
        addSnake();
        addStone();
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

    private void addApple() {
        graph.addVertex(apple);
        getNeighbours(apple, dw, dh, boardSize, barriers)
                .forEach(neighbourPoint ->
                {
                    graph.addVertex(neighbourPoint);
                    graph.addEdge(neighbourPoint, apple);
                });
    }

    private void addHead() {
        graph.addVertex(head);
        getNeighbours(head, dw, dh, boardSize, barriers)
                .forEach(neighbourPoint -> {
                    graph.addVertex(neighbourPoint);
                    graph.addEdge(head, neighbourPoint);
                });
    }

    private void addSnake() {
        snake.forEach(point ->
                graph.addVertex(point));
    }

    private void addStone() {
        graph.addVertex(stone);
    }

    private String solve() {
        logger.info("Inside solve()");
        long startMilli = Instant.now().toEpochMilli();
        Optional<Point> nextPoint = getNextPoint();
        if (nextPoint.isEmpty()) {
            String dir = Direction.random().toString();
            logger.fatal(String.format("Final Random direction returned: %s", dir));
            return dir;
        }
        String dir = PointHelper.getDir(head, nextPoint.get()).toString();
        logger.info(String.format("Final Direction found returned: %s", dir));
        logger.info(String.format("Time it took: %8.5f seconds", (Instant.now().toEpochMilli() - startMilli) / 1000.0));
        logger.info("Leaving solve()...");
        return dir;
    }

    private Optional<Point> getNextPoint() {
        Optional<Point> pointChosen = getDirToApple();
        if (pointChosen.isEmpty()) {
            pointChosen = getDirToFurthestEmptyPointNoStone();
            if (pointChosen.isEmpty()) {
                pointChosen = getDirToFurthestEmptyPointWithStone();
                if (pointChosen.isEmpty()) {
                    return Optional.empty();
                }
            }
        }
        return pointChosen;
    }

    public Optional<Point> getDirToApple() {
        logger.info("Inside getDirToApple():");
        DijkstraShortestPath<Point, DefaultEdge> path = new DijkstraShortestPath<>(graph, head, apple);
        return Optional.ofNullable(path.getPathEdgeList())
                .map(edgeList -> {
                    logger.info(String.format("Edge list: %s\n", edgeList));
                    return edgeList.get(0);
                })
                .map(edge -> {
                    Point point = graph.getEdgeTarget(edge);
                    logger.info(String.format("The point returned: %s", point));
                    logger.info("Leaving getDirToApple()..");
                    return point;
                });
    }

    public Optional<Point> getDirToFurthestEmptyPointNoStone() {
        logger.info("Inside getDirToFurthestEmptyPointNoStone():");
        return allPaths.getAllPaths(
                        Set.of(head), new HashSet<>(emptyPoints), true, null)
                .stream()
                .max(Comparator.comparingInt(GraphPath::getLength))
                .map(path -> {
                    List<Point> vertices = path.getVertexList();
                    Point nextPoint = path.getVertexList().get(1);
                    logger.info(String.format("Path found (vertices): %s", vertices));
                    logger.info(String.format("Point returned: %s", nextPoint));
                    logger.info("Leaving getDirToFurthestEmptyPointNoStone()...");
                    return nextPoint;
                });
    }

    public Optional<Point> getDirToFurthestEmptyPointWithStone() {
        logger.info("Inside getDirToFurthestEmptyPointWithStone():");
        Set<Point> pointsAvailable = new HashSet<>(emptyPoints);
        pointsAvailable.add(stone);
        return allPaths.getAllPaths(
                        Set.of(head), pointsAvailable, true, null)
                .stream()
                .max(Comparator.comparingInt(GraphPath::getLength))
                .map(path -> {
                    List<Point> vertices = path.getVertexList();
                    Point nextPoint = path.getVertexList().get(1);
                    logger.info(String.format("Path found (vertices): %s", vertices));
                    logger.info(String.format("Point returned: %s", nextPoint));
                    logger.info("Leaving getDirToFurthestEmptyPointWithStone()...");
                    return nextPoint;
                });
    }
}