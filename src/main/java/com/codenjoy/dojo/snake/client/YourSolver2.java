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

import static com.codenjoy.dojo.snake.client.PointHelper.areParallel;
import static com.codenjoy.dojo.snake.model.Elements.*;

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
    private BFSShortestPath<Point, DefaultEdge> shortestPaths;
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
            logger.printf(Level.INFO,
                    "Time it took: %8.5f seconds",
                    (Instant.now().toEpochMilli() - start) / 1000.0);
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
        this.barriers = board.getBarriers(); //includes walls, snake, stone
        this.walls = board.getWalls();
    }

    private void constructGraph() {
        this.graph = new DefaultDirectedWeightedGraph<>(DefaultEdge.class);
        this.allPaths = new AllDirectedPaths<>(graph);
        this.shortestPaths = new BFSShortestPath<>(graph);
        addEmptyPoints();
        addWeightToEmptyPoints();
        addApple();
        addHead();
        addWeightToHeadEdges();
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
                .forEach(neighbourPoint -> {
                    graph.addEdge(neighbourPoint, apple);
                    graph.addEdge(apple, neighbourPoint); //for the case going for stone
                });
    }

    private void addHead() {
        graph.addVertex(head);
        getNeighbours(head, barriers, false)
                .forEach(neighbourPoint -> graph.addEdge(head, neighbourPoint));
    }

    private void addHeadToStoneEdge() {
        ArrayList<Point> obstacles = new ArrayList<>(barriers);
        obstacles.remove(stone);
        getNeighbours(head, obstacles, false)
                .stream()
                .filter(point -> point.equals(stone))
                .forEach(neighbourPoint -> graph.addEdge(head, neighbourPoint));
    }

    private void addSnake() {
        snake.forEach(point -> graph.addVertex(point));
    }

    private String solve() {
        logger.printf(Level.INFO, "Inside solve()");
        long startMilli = Instant.now().toEpochMilli();
        logger.printf(Level.INFO, "Solution started at: %d epoc seconds", startMilli);
        Optional<Point> nextPoint = getNextPoint();
        if (nextPoint.isEmpty()) {
            String dir = Direction.random().toString();
            logger.printf(Level.FATAL, "Random result: %s", dir);
            return dir;
        }
        String dir = PointHelper.getDir(head, nextPoint.get()).toString(); //dir of nextPoint wrt. head
        logger.printf(Level.INFO, "Result: %s", dir);
        logger.printf(Level.INFO,
                "Solution finished. Time it took: %8.5f seconds",
                (Instant.now().toEpochMilli() - startMilli) / 1000.0);
        logger.printf(Level.INFO, "Leaving solve()...");
        return dir;
    }

    private Optional<Point> getNextPoint() {
        Optional<Point> nextPoint;
        Point pointChosen;
        if (snake.size() < 60) {
            pointChosen = apple;
        }
        else {
            logger.printf(Level.INFO, "Snake size was higher than 45: %d", snake.size());
            addStone();
            addWeightToStoneEdges();
            pointChosen = stone;
        }
        nextPoint = getDirToPoint(pointChosen);
        if (nextPoint.isEmpty()) {
            if (pointChosen.equals(apple)) {
                nextPoint = getDirToFurthestEmptyPointNoStone(15);
            }
            else {
                nextPoint = getDirToPoint(apple);
                if (nextPoint.isEmpty()) {
                    nextPoint = getDirToFurthestEmptyPointNoStone(15);
                }
            }
            if (nextPoint.isEmpty()) {
                nextPoint = getDirToFurthestEmptyPointWithStone(15);
                if (nextPoint.isEmpty()) {
                    return Optional.empty();
                }
            }
        }
        return nextPoint;
    }

    public Optional<Point> getDirToPoint(Point point) {
        logger.printf(Level.INFO, "Inside getDirToPoint():");
        if (point.equals(apple)) {
            logger.printf(Level.INFO, "Going for apple..");
        }
        else {
            logger.printf(Level.INFO, "Going for stone");
            addHeadToStoneEdge();
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
                    .map(emptyNeighbour -> {
                        return shortestPaths.getPath(head, emptyNeighbour);
                    })
                    .max(Comparator.comparingInt(path -> {
                        if (path == null) {
                            return - 1;
                        }
                        int weight = (int) path.getWeight();
                        logger.printf(Level.INFO, "Path to empty neighbour of the point: %s. Weight: %d",
                                path.getVertexList(),
                                weight);
                        return weight;
                    }))
                    .map(path -> {
                        List<Point> vertexList = path.getVertexList();
                        logger.printf(Level.INFO, "Path to empty neighbour of the point, decided: %s", vertexList);
                        return vertexList.get(1);
                    });
        }
        else {
            return getShortestPath(point);
        }
    }

    private Optional<Point> getShortestPath(Point point) {
        long startMilli = Instant.now().toEpochMilli();
        logger.printf(Level.INFO, "JohnsonShortestPath started at %d seconds", startMilli);
        return Optional.ofNullable(shortestPaths.getPath(head, point))
                .map(path -> {
                    List<Point> vertexList = path.getVertexList();
                    logger.printf(Level.INFO, "Shortest path to the point: %s", vertexList);
                    logger.printf(Level.INFO,
                            "JohnsonShortestPath took %8.5f seconds",
                            (Instant.now().toEpochMilli() -  startMilli) / 1000.0);
                    return vertexList.get(1);
                });
    }

    public Optional<Point> getDirToFurthestEmptyPointNoStone(Integer pathLength) {
        logger.printf(Level.INFO, "Inside getDirToFurthestEmptyPointNoStone():");
        return allPaths.getAllPaths(
                        Set.of(head), new HashSet<>(emptyPoints), true, pathLength)
                .stream()
                .max(/*(path1, path2) -> {
                    int flag = path1.getLength() - path2.getLength();
                    if (flag == 0) {
                        flag =  (int) (path1.getWeight() - path2.getWeight());
                    }
                    return flag;
                }*/Comparator.comparingInt(path -> (int) path.getWeight()))
                .map(path -> {
                    List<Point> vertexList = path.getVertexList();
                    logger.printf(Level.INFO, "Path to furthest empty point, decided: %s", vertexList);
                    logger.printf(Level.INFO, "Leaving getDirToFurthestEmptyPointNoStone()...");
                    return vertexList.get(1);
                });
    }

    public Optional<Point> getDirToFurthestEmptyPointWithStone(Integer pathLength) {
        logger.printf(Level.INFO, "Inside getDirToFurthestEmptyPointWithStone():");
        if (!graph.containsVertex(stone)) {
            addStone();
            addWeightToStoneEdges();
        }
        HashSet<Point> availablePoints = new HashSet<>(emptyPoints);
        availablePoints.add(stone);
        return allPaths.getAllPaths(
                        Set.of(head), new HashSet<>(availablePoints), true, pathLength)
                .stream()
                .max(/*(path1, path2) -> {
                    int flag = path1.getLength() - path2.getLength();
                    if (flag == 0) {
                        flag =  (int) (path1.getWeight() - path2.getWeight());
                    }
                    return flag;
                }*/Comparator.comparingInt(path -> (int) path.getWeight()))
                .map(path -> {
                    List<Point> vertexList = path.getVertexList();
                    logger.printf(Level.INFO, "Path to furthest empty point (inc. stone in way) found: %s", vertexList);
                    logger.printf(Level.INFO, "Leaving getDirToFurthestEmptyPointWithStone()...");
                    return vertexList.get(1);
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
                    if (/*barriers*/snake.contains(neighbourPoint) &&
                            !neighbourPoint.equals(head)
                            /*!neighbourPoint.equals(stone)*/) {
                        return 1;
                    }
                    return 0;
                })
                .sum();
        int targetWeight = getNeighbours(
                edgeTarget, null, true).stream()
                .mapToInt(neighbourPoint -> {
                    if (/*barriers*/snake.contains(neighbourPoint) &&
                            !neighbourPoint.equals(head)
                            /*!neighbourPoint.equals(stone)*/) {
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
        if (snake.size() > 35) {
            return true;
        }
        List<Point> allNeighbours = getNeighbours(point, null, true);
        List<Point> tails = board.get(
                TAIL_END_DOWN,
                TAIL_END_UP,
                TAIL_END_LEFT,
                TAIL_END_RIGHT,
                TAIL_HORIZONTAL,
                TAIL_LEFT_DOWN,
                TAIL_LEFT_UP,
                TAIL_RIGHT_DOWN,
                TAIL_RIGHT_UP,
                TAIL_VERTICAL);
        System.out.println("Neighbours of the point checked for risk:AW87" + allNeighbours);
        if (allNeighbours.contains(head)) {
            return false;
        }
        else {
            return allNeighbours.stream().anyMatch(point1 -> snake.contains(point1) || walls.contains(point1) /*&& !tails.contains(point1)*/); /*&&
                    allNeighbours.stream().noneMatch(point1 -> walls.contains(point1) && !tails.contains(point1));*/
        }
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
}