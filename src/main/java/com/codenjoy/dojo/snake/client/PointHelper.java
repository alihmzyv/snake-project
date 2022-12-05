package com.codenjoy.dojo.snake.client;

import com.codenjoy.dojo.services.Direction;
import com.codenjoy.dojo.services.Point;
import com.codenjoy.dojo.services.PointImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codenjoy.dojo.services.Direction.*;

public interface PointHelper {

    static List<Point> getNeighbours(Point point, int dw, int dh, int boardSize, List<Point> barriers, boolean outAllowed) {
        return Stream.of(change(point, UP),
                change(point, DOWN),
                change(point, RIGHT),
                change(point, LEFT))
                .filter(neighbourPoint ->
                {
                    if (outAllowed) {//all the neighbours except barriers
                        return (barriers == null || !barriers.contains(neighbourPoint));
                    }
                    return (!neighbourPoint.isOutOf(dw, dh, boardSize)) &&
                            (barriers == null || !barriers.contains(neighbourPoint)); //excluding the walls as well
                })
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

    static boolean isNeigbourOf(Point p1, Point p2) {
        int p1X = p1.getX();
        int p2X = p2.getX();
        int p1Y = p1.getY();
        int p2Y = p2.getY();
        return ((p1X == p2X - 1 && p1Y == p2Y) ||
                (p1X == p2X + 1 && p1Y == p2Y) ||
                (p1Y == p2Y - 1 && p1X == p2X) ||
                (p1Y == p2Y + 1 && p1X == p2X));
    }

    static boolean areParallel(Point p1, Point p2) {
        int p1X = p1.getX();
        int p2X = p2.getX();
        int p1Y = p1.getY();
        int p2Y = p2.getY();
        return p1X == p2X ||
                p1Y == p2Y;
    }
}
