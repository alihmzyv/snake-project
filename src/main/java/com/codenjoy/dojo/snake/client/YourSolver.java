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
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.shortestpath.BFSShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static com.codenjoy.dojo.snake.client.GraphHelper.*;
import static com.codenjoy.dojo.snake.client.PointHelper.*;

/**
 * User: your name
 */
public class YourSolver implements Solver<Board> {

    public static void main(String[] args) {
        logger.printf(Level.INFO, "RUN/TRY DATE/TIME: %s", LocalDateTime.now());
        try {
            WebSocketRunner.runClient(
                    // paste here board page url from browser after registration
                    "http://159.89.27.106/codenjoy-contest/board/player/oml71wa8arzdzmwqkgfi?code=4180714991633839453",
                    new YourSolver(new RandomDice()),
                    new Board());
        }
        catch (Exception exc) {
            exc.printStackTrace();
        }
    }
    private Dice dice;
    private Board board;
    private int boardSize;
    private int dh;
    private int dw;
    private int optimumSnakeSize = 35;
    private int maxSnakeSize = 70;
    private int optimumMaxPathLength = 15;

    private Point apple;
    private Point stone;
    private Point head;
    private List<Point> snake;
    private List<Point> emptyPoints;
    private List<Point> barriers;
    private static DefaultDirectedWeightedGraph<Point, DefaultEdge> graph;
    private AllDirectedPaths<Point, DefaultEdge> allPaths;
    private BFSShortestPath<Point, DefaultEdge> shortestPaths;
    private static final Logger logger = LogManager.getLogger("SnakeSolver");

    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    @Override
    public String get(Board board) {
        long start = Instant.now().toEpochMilli();
        logger.printf(Level.INFO,
                "Solution started");
        logger.printf(Level.INFO,
                board.toString());
        this.board = board;
        try {
            fetchData();
            String result = solve();
            logger.printf(Level.INFO,
                    "Solution finished. Time it took: %6.4f",
                    (Instant.now().toEpochMilli() - start) / 1000.0);
            logger.printf(Level.INFO,
                    "RESULT: %s",
                    result);
            return result;
        }
        catch (Exception exc) {
            exc.printStackTrace();
            return Direction.RIGHT.toString();
        }
    }

    private void fetchData() {
        parseBoard();
        constructGraph();
    }

    private void parseBoard() {
        this.boardSize = board.size();
        this.dh = 1;
        this.dw = 1;
        this.apple = board.getApples().get(0);
        this.stone = board.getStones().get(0);
        this.head = board.getHead();
        this.snake = board.getSnake();
        this.emptyPoints = board.get(Elements.NONE);
        this.barriers = board.getBarriers(); //includes walls, snake, stone
    }

    private void constructGraph() {
        graph = new DefaultDirectedWeightedGraph<>(DefaultEdge.class);
        addEmptyPoints();
        addHead();
        addApple();
        addSnake();
        this.allPaths = new AllDirectedPaths<>(graph);
        this.shortestPaths = new BFSShortestPath<>(graph);
    }

    private void addEmptyPoints() {
        emptyPoints.forEach(point -> {
            graph.addVertex(point);
            getNeighbours(point, barriers, false)
                    .forEach(neighbourPoint -> {
                        graph.addVertex(neighbourPoint);
                        DefaultEdge edge = graph.addEdge(point, neighbourPoint);
                        graph.setEdgeWeight(edge, calculateWeight(edge));
                    });
        });
    }

    private void addApple() {
        graph.addVertex(apple);
        getNeighbours(apple, barriers, false)
                .forEach(neighbourPoint -> {
                    DefaultEdge edge2 = graph.addEdge(apple, neighbourPoint);//for the case going for stone
                    graph.setEdgeWeight(edge2, calculateWeight(edge2));
                });
    }

    private void addHead() {
        graph.addVertex(head);
        getNeighbours(head, barriers, false)
                .forEach(neighbourPoint -> {
                    DefaultEdge edge = graph.addEdge(head, neighbourPoint);
                    graph.setEdgeWeight(edge, calculateWeight(edge));
                });
    }

    private void addSnake() {
        snake.forEach(point -> graph.addVertex(point));
    }

    private void addStone() {
        graph.addVertex(stone);
        ArrayList<Point> obstaclesNew = new ArrayList<>(barriers);
        obstaclesNew.remove(head);
        getNeighbours(stone, obstaclesNew, false)
                .forEach(neighbourPoint ->
                {
                    DefaultEdge edge1 = graph.addEdge(stone, neighbourPoint);
                    graph.setEdgeWeight(edge1, calculateWeight(edge1));
                    DefaultEdge edge2 = graph.addEdge(neighbourPoint, stone);
                    graph.setEdgeWeight(edge2, calculateWeight(edge2));
                });
    }

    private double calculateWeight(DefaultEdge edge) {
        Point edgeSource = graph.getEdgeSource(edge);
        Point edgeTarget = graph.getEdgeTarget(edge);
        int sourceWeight = getNeighbours(
                edgeSource, null, true)
                .stream()
                .mapToInt(neighbourPoint -> {
                    if (barriers.contains(neighbourPoint) &&
                            !neighbourPoint.equals(head) &&
                            !neighbourPoint.equals(stone)) {
                        return 0;
                    }
                    return 1;
                })
                .sum();
        int targetWeight = getNeighbours(
                edgeTarget, null, true)
                .stream()
                .mapToInt(neighbourPoint -> {
                    if (barriers.contains(neighbourPoint) &&
                            !neighbourPoint.equals(head) &&
                            !neighbourPoint.equals(stone)) {
                        return 0;
                    }
                    return 1;
                })
                .sum();
        return sourceWeight + targetWeight;
    }

    private String solve() {
        Point pointChosen = chooseAppleOrStone();
        Optional<Point> nextPoint = getNextPointFor(pointChosen);
        if (nextPoint.isEmpty()) {
            if (pointChosen.equals(apple)) {
                logger.printf(Level.INFO, "There was no path to apple, wander around without eating stone..");
                nextPoint = getDirToFurthestEmptyPointNoStone(optimumMaxPathLength);
            }
            else {
                logger.printf(Level.INFO, "There was no path to stone, try going for apple..");
                nextPoint = getNextPointFor(apple);
                if (nextPoint.isEmpty()) {
                    logger.printf(Level.INFO, "There was no path to apple either, wander around without eating stone..");
                    nextPoint = getDirToFurthestEmptyPointNoStone(optimumMaxPathLength);
                }
            }
            if (nextPoint.isEmpty()) {
                logger.printf(Level.INFO, "There was no path at all, wander around eating stone even !");
                nextPoint = getDirToFurthestEmptyPointWithStone(optimumMaxPathLength);
            }
        }
        return getDirWrtHead(nextPoint.orElse(null));
    }

    private String getDirWrtHead(Point neighbourOfHead) {
        if (neighbourOfHead == null) {
            logger.printf(Level.INFO, "There was no path found at all. RESULT WILL BE RANDOM!");
            return Direction.random().toString();
        }
        return PointHelper.getDir(head, neighbourOfHead).toString();
    }

    private Point chooseAppleOrStone() {
        if (snake.size() < maxSnakeSize) {
            logger.printf(Level.INFO, "Apple was chosen.");
            return apple;
        }
        else {
            logger.printf(Level.INFO, "Stone was chosen.");
            return stone;
        }
    }

    public Optional<Point> getNextPointFor(Point point) {
        if (point.equals(stone)) {
            addStone();
        }

        if (isNeigbourOf(head, point)) {//if head is near the point, eat it !
            logger.printf(Level.INFO, "Apple or stone is the neighbour of head, EAT IT !");
            return Optional.of(point);
        }
        else if (isOnDeadPoint(point)) { //surrounded by 3 barriers
            logger.printf(Level.INFO, "Apple or stone is on a dead point.");
            return Optional.empty();
        }
        if (snake.size() >= optimumSnakeSize) {
            logger.printf(Level.INFO, "Snake size is greater than the optimum size. Going for the lightest path to apple or stone");
            Optional<Point> pointOpt = getLightestDirToEmptyNeighbourOfPoint(point);
            if (pointOpt.isPresent()) {
                return pointOpt;
            }
            return getShortestLightestDirToEmptyNeighbourOfPoint(point);
        }
        else {
            logger.printf(Level.INFO, "Snake size is OK. Going for the shortest, lightest path to the apple or stone.");
            return getShortestLightestDirToEmptyNeighbourOfPoint(point);
        }
    }

    private Optional<Point> getLightestDirToEmptyNeighbourOfPoint(Point point) {
        logger.printf(Level.INFO,
                "Inside getLightestDirToEmptyNeighbourOfPoint()");
        List<Point> emptyNeighbours = getNeighbours(point, barriers, false);
        return allPaths.getAllPaths(Set.of(head),
                        new HashSet<>(emptyNeighbours),
                        true,
                        optimumMaxPathLength)
                .stream()
                .min(Comparator.comparingInt(path -> (int) getPathWeight(path)))
                .map(path -> {
                    if (snake.size() >= optimumSnakeSize && pathIsDangerous(path)) {
                        logger.printf(Level.WARN,
                                "Path was dangerous.");
                        logger.printf(Level.INFO,
                                "Leaving getLightestDirToEmptyNeighbourOfPoint()");
                        return null;
                    }
                    List<Point> vertexList = getVertexList(path);
                    logger.printf(Level.INFO, "Chosen path: %s", vertexList);
                    logger.printf(Level.INFO,
                            "Inside getLightestDirToEmptyNeighbourOfPoint()");
                    return vertexList.get(1);
                });
    }

    public Optional<Point> getShortestLightestDirToEmptyNeighbourOfPoint(Point point) {
        logger.printf(Level.INFO, "Inside getShortestLightestDirToEmptyNeighbourOfPoint()");
        return getNeighbours(point, barriers, false)
                .stream()
                .map(emptyNeighbour -> shortestPaths.getPath(head, emptyNeighbour))
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt((GraphPath<Point, DefaultEdge> path) -> getPathLength(path))
                        .thenComparingInt((GraphPath<Point, DefaultEdge> path) -> (int) getPathWeight(path)))
                .map(path -> {
                    if (snake.size() >= optimumSnakeSize && pathIsDangerous(path)) {
                        logger.printf(Level.INFO,
                                "Path was dangerous.");
                        logger.printf(Level.INFO,
                                "Leaving getShortestLightestDirToEmptyNeighbourOfPoint()");
                        return null;
                    }
                    logger.printf(Level.INFO,
                            "Leaving getShortestLightestDirToEmptyNeighbourOfPoint()");
                    return getVertexList(path).get(1);
                });
    }

    public Optional<Point> getDirToFurthestEmptyPointNoStone(Integer pathLength) {
        logger.printf(Level.INFO, "Inside getDirToFurthestEmptyPointNoStone()");
        //find the empty point that is furthest and there is a path between head and that
        Optional<Point> furthestAccessibleEmptyPoint = emptyPoints
                .stream()
                .filter(emptyPoint -> {
                    GraphPath<Point, DefaultEdge> path = shortestPaths.getPath(head, emptyPoint);
                    return path != null && (snake.size() < optimumSnakeSize || !pathIsDangerous(path)) && path.getLength() <= pathLength / 4;
                })
                .max(Comparator.comparingDouble(emptyPoint -> getDistanceBetween(head, emptyPoint)));
        if (furthestAccessibleEmptyPoint.isEmpty()) {
            logger.printf(Level.INFO, "There is no empty point or stone accessible.");
            logger.printf(Level.INFO, "Leaving getDirToFurthestEmptyPointNoStone()");
            return Optional.empty();
        }
        return allPaths.getAllPaths(
                        head,
                        furthestAccessibleEmptyPoint.get(),
                        true,
                        optimumMaxPathLength)
                .stream()
                .max(Comparator
                        .comparingInt((GraphPath<Point, DefaultEdge> path) -> getPathLength(path))
                        .thenComparingInt(path -> - (int) getPathWeight(path)))
                .map(path -> {
                    if (snake.size() >= optimumSnakeSize && pathIsDangerous(path)) {
                        logger.printf(Level.INFO, "Path was dangerous.");
                        logger.printf(Level.INFO, "Leaving getDirToFurthestEmptyPointNoStone()");
                        return null;
                    }
                    logger.printf(Level.INFO, "Leaving getDirToFurthestEmptyPointNoStone()");
                    return getVertexList(path).get(1);
                });
    }

    public Optional<Point> getDirToFurthestEmptyPointWithStone(Integer pathLength) {
        logger.printf(Level.INFO, "Inside getDirToFurthestEmptyPointWithStone()");
        addStone();
        HashSet<Point> availablePoints = new HashSet<>(emptyPoints);
        availablePoints.add(stone);
        Optional<Point> furthestAccessibleEmptyPoint = availablePoints
                .stream()
                .filter(emptyPoint -> {
                    GraphPath<Point, DefaultEdge> path = shortestPaths.getPath(head, emptyPoint);
                    return path != null && (snake.size() < optimumSnakeSize || !pathIsDangerous(path)) && path.getLength() <= pathLength / 4;
                })
                .max(Comparator.comparingDouble(emptyPoint -> getDistanceBetween(head, emptyPoint)));
        if (furthestAccessibleEmptyPoint.isEmpty()) {
            logger.printf(Level.INFO, "There is no empty point or stone accessible.");
            logger.printf(Level.INFO, "Leaving getDirToFurthestEmptyPointWithStone()");
            return Optional.empty();
        }
        return allPaths.getAllPaths(
                        head,
                        furthestAccessibleEmptyPoint.get(),
                        true,
                        optimumMaxPathLength)
                .stream()
                .max(Comparator
                        .comparingInt((GraphPath<Point, DefaultEdge> path) -> getPathLength(path))
                        .thenComparingInt(path -> - (int) getPathWeight(path)))
                .map(path -> {
                    if (snake.size() >= optimumSnakeSize && pathIsDangerous(path)) {
                        logger.printf(Level.INFO, "Path was dangerous");
                        logger.printf(Level.INFO, "Leaving getDirToFurthestEmptyPointWithStone()");
                        return null;
                    }
                    logger.printf(Level.INFO, "Leaving getDirToFurthestEmptyPointWithStone()");
                    return getVertexList(path).get(1);
                });
    }

    private List<Point> getNeighbours(Point point, List<Point> barriers, boolean outAllowed) {
        return PointHelper.getNeighbours4D(point, dw, dh, boardSize, barriers, outAllowed);
    }

    private boolean isOnDeadPoint(Point point) {
        List<Point> allBarrierNeighbours = getNeighbours(point, null, true)
                .stream()
                .filter(point1 -> barriers.contains(point1) &&
                        !point1.equals(head))
                .toList();
        return allBarrierNeighbours.size() == 3 ||
                (allBarrierNeighbours.size() == 2 &&
                        areParallel(allBarrierNeighbours.get(0), allBarrierNeighbours.get(1)));
    }

    private boolean pathIsDangerous(GraphPath<Point, DefaultEdge> path) {
        return path.getVertexList()
                .stream()
                .filter(point -> !point.equals(head))
                .anyMatch(this::isOnDeadPoint);
    }
}