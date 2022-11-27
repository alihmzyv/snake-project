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
import com.codenjoy.dojo.snake.model.Elements;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.shortestpath.JohnsonShortestPaths;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static com.codenjoy.dojo.snake.client.PointHelper.areParallel;

/**
 * User: your name
 */
public class YourSolver2 implements Solver<Board> {

    public static void main(String[] args) {
        logger.printf(Level.INFO, "RUN/TRY DATE/TIME: %s", LocalDateTime.now());
        try {
            WebSocketRunner.runClient(
                    // paste here board page url from browser after registration
                    "http://159.89.27.106/codenjoy-contest/board/player/oml71wa8arzdzmwqkgfi?code=4180714991633839453",
                    new YourSolver2(new RandomDice()),
                    new Board());
        }
        catch (Exception exc) {
            logger.printf(Level.FATAL, "Exception trace: %s", Arrays.stream(exc.getStackTrace()).toArray());
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
    private List<Point> walls;
    private DefaultDirectedWeightedGraph<Point, DefaultEdge> graph;
    private AllDirectedPaths<Point, DefaultEdge> allPaths;
    private JohnsonShortestPaths<Point, DefaultEdge> shortestPaths;
    private static final Logger logger = LogManager.getLogger("SnakeSolver");

    public YourSolver2(Dice dice) {
        this.dice = dice;
    }

    @Override
    public String get(Board board) {
        logger.printf(Level.INFO, "Inside get(Board board)");
        this.board = board;
        long start = Instant.now().toEpochMilli();
        try {
            fetchData();
            return solve();
        }
        catch (Exception exc) {
            logger.fatal(exc.getMessage());
            logger.printf(Level.INFO, "Exception thrown in get(). RIGHT Dir. returned.");
            logger.printf(Level.INFO, "Time it took: %8.5f seconds", (Instant.now().toEpochMilli() - start) / 1000.0);
            logger.printf(Level.INFO, "Leaving get(Board board)");
            return Direction.RIGHT.toString();
        }
    }

    private void fetchData() {
        parseBoard();
        constructGraph();
    }

    private void parseBoard() {
        logger.printf(Level.INFO, "The board:\n%s\n", board);
        this.boardSize = board.size();
        this.dh = 1;
        this.dw = 1;
        this.apple = board.getApples().get(0);
        this.stone = board.getStones().get(0);
        this.head = board.getHead();
        this.snake = board.getSnake();
        this.emptyPoints = board.get(Elements.NONE);
        this.barriers = board.getBarriers();
        this.walls = board.getWalls();
    }

    private void constructGraph() {
        this.graph = new DefaultDirectedWeightedGraph<>(DefaultEdge.class);
        this.allPaths = new AllDirectedPaths<>(graph);
        this.shortestPaths = new JohnsonShortestPaths<>(graph);
        addEmptyPoints();
        addApple();
        addHead();
        addSnake();
    }

    private void addEmptyPoints() {
        emptyPoints.forEach(point -> {
            graph.addVertex(point);
            getNeighbours(point, barriers, false)
                    .forEach(neighbourPoint -> {
                        graph.addVertex(neighbourPoint);
                        graph.addEdge(point, neighbourPoint);
                    });
        });
    }

    private void addApple() {
        graph.addVertex(apple);
        getNeighbours(apple, barriers, false)
                .forEach(neighbourPoint -> graph.addEdge(neighbourPoint, apple));
    }

    private void addHead() {
        graph.addVertex(head);
        getNeighbours(head, barriers, false)
                .forEach(neighbourPoint -> graph.addEdge(head, neighbourPoint));
    }

    private void addSnake() {
        snake.forEach(point -> graph.addVertex(point));
    }

    private String solve() {
        logger.printf(Level.INFO, "Inside solve()");
        long startMilli = Instant.now().toEpochMilli();
        logger.printf(Level.INFO, "Solve started: %d epoc seconds", startMilli);
        Optional<Point> nextPoint = getNextPoint();
        if (nextPoint.isEmpty()) {
            String dir = Direction.random().toString();
            logger.printf(Level.FATAL, "Final Random direction returned: %s", dir);
            return dir;
        }
        String dir = PointHelper.getDir(head, nextPoint.get()).toString();
        logger.printf(Level.INFO, "Final Direction found returned: %s", dir);
        logger.printf(Level.INFO, "Solve Finished. Time it took: %8.5f seconds",
                (Instant.now().toEpochMilli() - startMilli) / 1000.0);
        logger.printf(Level.INFO, "Leaving solve()...");
        return dir;
    }

    private Optional<Point> getNextPoint() {
        Optional<Point> pointChosen;
        if (snake.size() < 45) {
            addWeightToEmptyPoints();
            addWeightToHeadEdges();
            pointChosen = getDirToPoint(apple);
        }
        else {
            logger.printf(Level.INFO, "Snake size was higher than 45: %d", snake.size());
            addWeightToEmptyPoints();
            addWeightToHeadEdges();
            addStone();
            addWeightToStoneEdges();
            pointChosen = getDirToPoint(stone);
        }
        if (pointChosen.isEmpty()) {
            pointChosen = getDirToFurthestEmptyPointNoStone(15);
            if (pointChosen.isEmpty()) {
                pointChosen = getDirToFurthestEmptyPointWithStone(15);
                if (pointChosen.isEmpty()) {
                    return Optional.empty();
                }
            }
        }
        return pointChosen;
    }

    public Optional<Point> getDirToPoint(Point point) {
        logger.printf(Level.INFO, "Inside getDirToPoint():");
        if (point.equals(apple)) {
            logger.printf(Level.INFO, "Going for apple..");
        }
        else {
            logger.printf(Level.INFO, "Going for stone");
        }
        if (isOnDeadPoint(point)) {
            logger.warn("The apple or stone is on a dead point. Wandering around..");
            return Optional.empty();
        }
        else if (isRisky(point)) {
            logger.warn("Point is risky");
            List<Point> allEmptyNeighbours = getNeighbours(point, barriers, false);
            logger.printf(Level.INFO, "Empty neighbours of the point: %s", allEmptyNeighbours);
            return allEmptyNeighbours.stream()
                    .map(emptyNeighbour -> shortestPaths.getPath(head, emptyNeighbour))
                    .filter(Objects::nonNull)
                    .max(Comparator.comparingInt(path -> (int) path.getWeight()))
                    .map(path -> {
                        logger.printf(Level.INFO, "Path found: %s", path.getVertexList());
                        return graph.getEdgeTarget(path.getEdgeList().get(0));
                    });
        }
        else {
            return getShortestPath(point);
        }
    }

    private Optional<Point> getShortestPath(Point point) {
        return Optional.ofNullable(shortestPaths.getPath(head, point))
                .map(path -> {
                    logger.printf(Level.INFO, "Shortest path to apple: %s", path.getVertexList());
                    return graph.getEdgeTarget(path.getEdgeList().get(0));
                });
    }

    public Optional<Point> getDirToFurthestEmptyPointNoStone(Integer pathLength) {
        logger.printf(Level.INFO, "Inside getDirToFurthestEmptyPointNoStone():");
        addWeightToEmptyPoints();
        addWeightToHeadEdges();
        return allPaths.getAllPaths(
                        Set.of(head), new HashSet<>(emptyPoints), true, pathLength)
                .stream()
                .max((path1, path2) -> {
                    int flag = path1.getLength() - path2.getLength();
                    if (flag == 0) {
                        flag =  (int) (path1.getWeight() - path2.getWeight());
                    }
                    return flag;
                })
                .map(path -> {
                    List<Point> vertices = path.getVertexList();
                    logger.printf(Level.INFO, "Path found: %s", vertices);
                    Point nextPoint = vertices.get(1);
                    logger.printf(Level.INFO, "Point returned: %s", nextPoint);
                    logger.printf(Level.INFO, "Leaving getDirToFurthestEmptyPointNoStone()...");
                    return path.getVertexList().get(1);
                });
    }

    public Optional<Point> getDirToFurthestEmptyPointWithStone(Integer pathLength) {
        logger.printf(Level.INFO, "Inside getDirToFurthestEmptyPointWithStone():");
        addWeightToEmptyPoints();
        addWeightToHeadEdges();
        addStone();
        addWeightToStoneEdges();
        HashSet<Point> availablePoints = new HashSet<>(emptyPoints);
        getNeighbours(stone, barriers, false)
                .forEach(neighbourPoint -> graph.addEdge(neighbourPoint, stone));
        availablePoints.add(stone);
        return allPaths.getAllPaths(
                        Set.of(head), new HashSet<>(availablePoints), true, pathLength)
                .stream()
                .max((path1, path2) -> {
                    int flag = path1.getLength() - path2.getLength();
                    if (flag == 0) {
                        flag =  (int) (path1.getWeight() - path2.getWeight());
                    }
                    return flag;
                })
                .map(path -> {
                    List<Point> vertices = path.getVertexList();
                    logger.printf(Level.INFO, "Path found: %s", vertices);
                    Point nextPoint = vertices.get(1);
                    logger.printf(Level.INFO, "Point returned: %s", nextPoint);
                    logger.printf(Level.INFO, "Leaving getDirToFurthestEmptyPointWithStone()...");
                    return path.getVertexList().get(1);
                });
    }

    private List<Point> getNeighbours(Point point, List<Point> barriers, boolean outAllowed) {
        return PointHelper.getNeighbours(point, dw, dh, boardSize, barriers, outAllowed);
    }

    private double calculateWeight(DefaultEdge edge) {
        Point edgeSource = graph.getEdgeSource(edge);
        Point edgeTarget = graph.getEdgeTarget(edge);
        int sourceWeight = getNeighbours(
                edgeSource, null, true).stream()
                .mapToInt(neighbourPoint -> {
                    if (barriers.contains(neighbourPoint) &&
                            !neighbourPoint.equals(head) &&
                            !neighbourPoint.equals(head)) {
                        return 1;
                    }
                    return 0;
                })
                .sum();
        int targetWeight = getNeighbours(
                edgeTarget, null, true).stream()
                .mapToInt(neighbourPoint -> {
                    if (barriers.contains(neighbourPoint) &&
                            !neighbourPoint.equals(head) &&
                            !neighbourPoint.equals(head)) {
                        return 1;
                    }
                    return 0;
                })
                .sum();
        return sourceWeight + targetWeight;
    }

    private void addWeightToEmptyPoints() {
        emptyPoints
                .forEach(point -> getNeighbours(point, barriers, false)
                        .forEach(neighbourPoint -> {
                            DefaultEdge edge = graph.getEdge(point, neighbourPoint);
                            graph.setEdgeWeight(edge, calculateWeight(edge));
                        }));
    }

    private void addWeightToHeadEdges() {
        getNeighbours(head, barriers, false)
                .forEach(neighbourPoint -> {
                    DefaultEdge edge = graph.getEdge(head, neighbourPoint);
                    graph.setEdgeWeight(edge, calculateWeight(edge));
                });
    }

    private void addStone() {
        graph.addVertex(stone);
        getNeighbours(stone, barriers, false)
                .forEach(neighbourPoint ->
                {
                    graph.addEdge(stone, neighbourPoint);
                    graph.addEdge(neighbourPoint, stone);
                });
    }

    private void addWeightToStoneEdges() {
        getNeighbours(stone, barriers, false)
                .forEach(neighbourPoint ->
                {
                    DefaultEdge edge = graph.getEdge(stone, neighbourPoint);
                    DefaultEdge edge2 = graph.getEdge(neighbourPoint, stone);
                    graph.setEdgeWeight(edge, calculateWeight(edge));
                    graph.setEdgeWeight(edge2, calculateWeight(edge2));
                });
    }

    private boolean isRisky(Point point) {
        List<Point> allNeighbours = getNeighbours(point, null, true);
        if (allNeighbours.contains(head)) {
            return false;
        }
        else return allNeighbours
                .stream()
                .anyMatch(point1 -> snake.contains(point1) || walls.contains(point1));
    }

    private boolean isOnDeadPoint(Point point) {
        List<Point> allBarrierNeighbours = getNeighbours(point, null, true)
                .stream()
                .filter(point1 -> barriers.contains(point1) && !point1.equals(head))
                .toList();
        return allBarrierNeighbours.size() == 3 ||
                (allBarrierNeighbours.size() == 2 &&
                        new HashSet<>(snake).containsAll(allBarrierNeighbours) &&
                        areParallel(allBarrierNeighbours.get(0), allBarrierNeighbours.get(1)));
    }
}