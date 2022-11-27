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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.alg.shortestpath.JohnsonShortestPaths;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.time.Instant;
import java.util.*;

import static com.codenjoy.dojo.snake.client.PointHelper.getNeighbours;

/**
 * User: your name
 */
public class YourSolver implements Solver<Board> {


    public static void main(String[] args) {
//        logger.info(String.format("RUN/TRY DATE/TIME: %s", LocalDateTime.now()));
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
    private JohnsonShortestPaths<Point, DefaultEdge> shortestPaths;
    private static final Logger logger = LogManager.getLogger("SnakeSolver");

    public YourSolver(Dice dice) {
        this.dice = dice;
    }

    @Override
    public String get(Board board) {
//        logger.info("Inside get(Board board)");
        this.board = board;
//        long start = Instant.now().toEpochMilli();
        try {
            fetchData();
            return solve();
        }
        catch (Exception exc) {
//            logger.error(exc.getMessage());
//            logger.info("Exception thrown in get(). RIGHT Dir. returned.");
//            logger.info(String.format("Time it took: %8.5f seconds", (Instant.now().toEpochMilli() - start) / 1000.0));
//            logger.info("Leaving get(Board board)");
            return Direction.RIGHT.toString();
        }
    }

    private void fetchData() {
        parseBoard();
        constructGraph();
    }

    private void parseBoard() {
//        logger.info(String.format("The board:\n%s\n", board));
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
        this.shortestPaths = new JohnsonShortestPaths<>(graph);
        addEmptyPoints();
        addApple();
        addHead();
        addSnake();
        addStone();
        System.out.println(graph.toString());
    }

    private void addEmptyPoints() {
        emptyPoints.forEach(point -> {
            graph.addVertex(point);
            getNeighbours(point, dw, dh, boardSize, barriers, false)
                    .forEach(neighbourPoint -> {
                        graph.addVertex(neighbourPoint);
                        graph.addEdge(point, neighbourPoint);
                    });
        });
    }

    private void addApple() {
        graph.addVertex(apple);
        getNeighbours(apple, dw, dh, boardSize, barriers, false)
                .forEach(neighbourPoint ->
                {
                    graph.addVertex(neighbourPoint);
                    graph.addEdge(neighbourPoint, apple);
                });
    }

    private void addHead() {
        graph.addVertex(head);
        getNeighbours(head, dw, dh, boardSize, barriers, false)
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
        getNeighbours(stone, dw, dh, boardSize, barriers, false)
                .forEach(neighbourPoint ->
                {
                    graph.addVertex(neighbourPoint);
                    graph.addEdge(stone, neighbourPoint);
                });
    }

    private String solve() {
//        logger.info("Inside solve()");
        long startMilli = Instant.now().toEpochMilli();
//        logger.info(String.format("Solve started: %d epoc seconds", startMilli));
        Optional<Point> nextPoint = getNextPoint();
        if (nextPoint.isEmpty()) {
            String dir = Direction.random().toString();
//            logger.fatal(String.format("Final Random direction returned: %s", dir));
            return dir;
        }
        String dir = PointHelper.getDir(head, nextPoint.get()).toString();
//        logger.info(String.format("Final Direction found returned: %s", dir));
//        logger.info(String.format("Solve Finished. Time it took: %8.5f seconds", (Instant.now().toEpochMilli() - startMilli) / 1000.0));
//        logger.info("Leaving solve()...");
        return dir;
    }

    private Optional<Point> getNextPoint() {
        Optional<Point> pointChosen;
        if (snake.size() > 70) {
//            logger.info(String.format("Snake size was higher than 70: %d", snake.size()));
            pointChosen = getDirToPoint(apple);
        }
        else {
            pointChosen = getDirToPoint(apple);
        }
        if (pointChosen.isEmpty()) {
            pointChosen = getDirToFurthestEmptyPointNoStone(null);
            if (pointChosen.isEmpty()) {
                pointChosen = getDirToFurthestEmptyPointWithStone(null);
                if (pointChosen.isEmpty()) {
                    return Optional.empty();
                }
            }
        }
        return pointChosen;
    }

    public Optional<Point> getDirToPoint(Point point) {
//        logger.info("Inside getDirToPoint():");
//        if (point.equals(apple)) {
//            logger.info("Going for apple..");
//        }
//        else {
//            logger.info("Going for stone");
//        }
        List<Point> obstacles = new ArrayList<>(barriers);
        obstacles.remove(head);
        if (getNeighbours(point, dw, dh, boardSize, obstacles, false).size() == 1 &&
                !getNeighbours(point, dw, dh, boardSize, obstacles, false).contains(head)) {
//            logger.info("The apple or stone is on the dead point. Not going..");
            return Optional.empty();
        }
        return Optional.ofNullable(shortestPaths.getPath(head, point))
                .map(path -> graph.getEdgeTarget(path.getEdgeList().get(0)));
    }

    public Optional<Point> getDirToFurthestEmptyPointNoStone(Integer pathLength) {
//        logger.info("Inside getDirToFurthestEmptyPointNoStone():");
        return allPaths.getAllPaths(
                        Set.of(head), new HashSet<>(emptyPoints), true, pathLength)
                .stream()
                .max(Comparator.comparingInt(GraphPath::getLength))
//                .min(Comparator.comparing(path -> {
//                    AtomicInteger curvy = new AtomicInteger();
//                    path.getEdgeList()
//                            .forEach(edge -> {
//                                Point source = graph.getEdgeSource(edge);
//                                Point target = graph.getEdgeTarget(edge);
//                                if (source.getY() == target.getY()) {
//                                    curvy.getAndIncrement();
//                                }
//                                else {
//                                    curvy.getAndDecrement();
//                                }
//                            });
//                    return curvy.get();
//                }))
                .map(path -> {
//                    List<Point> vertices = path.getVertexList();
                    //                    logger.info(String.format("Path found: %s", vertices));
//                    logger.info(String.format("Point returned: %s", nextPoint));
//                    logger.info("Leaving getDirToFurthestEmptyPointNoStone()...");
                    return path.getVertexList().get(1);
                });
    }

    public Optional<Point> getDirToFurthestEmptyPointWithStone(Integer pathLength) {
//        logger.info("Inside getDirToFurthestEmptyPointWithStone():");
        HashSet<Point> availablePoints = new HashSet<>(emptyPoints);
        getNeighbours(stone, dw, dh, boardSize, barriers, false)
                .forEach(neighbourPoint -> graph.addEdge(neighbourPoint, stone));
        availablePoints.add(stone);
        return allPaths.getAllPaths(
                        Set.of(head), new HashSet<>(availablePoints), true, pathLength)
                .stream()
                .max(Comparator.comparingInt(GraphPath::getLength))
//                .filter(path -> path.getLength() == pathLength)
//                .min(Comparator.comparingInt(path -> {
//                    AtomicInteger curvy = new AtomicInteger();
//                    path.getEdgeList()
//                            .forEach(edge -> {
//                                Point source = graph.getEdgeSource(edge);
//                                Point target = graph.getEdgeTarget(edge);
//                                if (source.getY() == target.getY()) {
//                                    curvy.getAndIncrement();
//                                }
//                                else {
//                                    curvy.getAndDecrement();
//                                }
//                            });
//                    return curvy.get();
//                }))
                .map(path -> {
//                    List<Point> vertices = path.getVertexList();
                    //                    logger.info(String.format("Path found: %s", vertices));
//                    logger.info(String.format("Point returned: %s", nextPoint));
//                    logger.info("Leaving getDirToFurthestEmptyPointWithStone()...");
                    return path.getVertexList().get(1);
                });
    }
}