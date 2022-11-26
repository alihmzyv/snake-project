package com.codenjoy.dojo.snake.client;

import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.PointImpl;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codenjoy.dojo.services.Direction.*;

public interface PointHelper {

    static List<Point> getNeighbours(Point point, int dw, int dh, int boardSize, List<Point> barriers) {
        return Stream.of(change(point, UP),
                change(point, DOWN),
                change(point, RIGHT),
                change(point, LEFT))
                .filter(neighbourPoint ->
                        (!neighbourPoint.isOutOf(dw, dh, boardSize)) &&
                        !barriers.contains(neighbourPoint))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    static Point change(Point point, Direction dir) {
        PointImpl pointCopy = new PointImpl(point);
        pointCopy.change(dir);
        return pointCopy;
    }

    static Direction getDir(Point point1, Point point2) {
        if (point2.equals(point1)) {
            return STOP;
        }
        int point1X = point1.getX();
        int point1Y = point1.getY();
        int point2X = point2.getX();
        int point2Y = point2.getY();
        if (point2X < point1X) {
            return LEFT;
        }
        else if (point2X > point1X) {
            return RIGHT;
        }
        else if (point2Y < point1Y) {
            return DOWN;
        }
        else {
            return UP;
        }
    }
}
