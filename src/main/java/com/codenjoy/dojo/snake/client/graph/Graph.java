package com.codenjoy.dojo.snake.client.graph;

import javax.swing.text.html.Option;
import java.util.*;

public class Graph<A> {
    private Set<Node<A>> nodes;
    private boolean directed;

    public Graph(boolean directed) {
        this.directed = directed;
        nodes = new HashSet<>();
    }

    public Set<Node<A>> getAllNodes() {
        return nodes;
    }

    public Optional<Node<A>> getNode(Node<A> node) {
        return getAllNodes()
                .stream()
                .filter(node1 -> node1.equals(node))
                .findAny();
    }

    // Doesn't need to be called for any node that has an edge to another node
// since addEdge makes sure that both nodes are in the nodes Set
    public void addNode(Node<A>... n) {
        // We're using a var arg method so we don't have to call
        // addNode repeatedly
        nodes.addAll(Arrays.asList(n));
    }

    public void addNode(Node<A> node) {
        // We're using a var arg method so we don't have to call
        // addNode repeatedly
        nodes.add(node);
    }

    public void addAllNodes(List<Node<A>> nodes) {
        // We're using a var arg method so we don't have to call
        // addNode repeatedly
        this.nodes.addAll(nodes);
    }

    public void addEdge(Node<A> source, Node<A> destination, double weight) {
        // Since we're using a Set, it will only add the nodes
        // if they don't already exist in our graph
        nodes.add(source);
        nodes.add(destination);

        // We're using addEdgeHelper to make sure we don't have duplicate edges
        addEdgeHelper(source, destination, weight);

        if (!directed && !source.equals(destination)) {
            addEdgeHelper(destination, source, weight);
        }
    }

    private void addEdgeHelper(Node<A> a, Node<A> b, double weight) {
        // Go through all the edges and see whether that edge has
        // already been added
        for (Edge<A> edge : a.edges) {
            if (edge.source.equals(a) && edge.destination.equals(b)) {
                // Update the value in case it's a different one now
                edge.weight = weight;
                return;
            }
        }
        // If it hasn't been added already (we haven't returned
        // from the for loop), add the edge
        a.edges.add(new Edge<>(a, b, weight));
    }

    public void printEdges() {
        for (Node<A> node : nodes) {
            LinkedList<Edge<A>> edges = node.edges;

            if (edges.isEmpty()) {
                System.out.println("Node " + node + " has no edges.");
                continue;
            }
            System.out.print("Node " + node + " has edges to: ");

            for (Edge<A> edge : edges) {
                System.out.print(edge.destination + "(" + edge.weight + ") ");
            }
            System.out.println();
        }
    }

    public boolean hasEdge(Node<A> source, Node<A> destination) {
        LinkedList<Edge<A>> edges = source.edges;
        for (Edge<A> edge : edges) {
            // Again relying on the fact that all classes share the
            // exact same Node object
            if (edge.destination.equals(destination)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNode(Node<A> node) {
        return this.nodes.contains(node);
    }

    // Necessary call if we want to run the algorithm multiple times
    public void resetNodesVisited() {
        for (Node<A> node : nodes) {
            node.unvisit();
        }
    }

    public List<Node<A>> DijkstraShortestPath(Node<A> start, Node<A> end) {
//        // We keep track of which path gives us the shortest path for each node
//        // by keeping track how we arrived at a particular node, we effectively
//        // keep a "pointer" to the parent node of each node, and we follow that
//        // path to the start
//        List<Node<A>> result = new ArrayList<>();
//        HashMap<Node<A>, Node<A>> changedAt = new HashMap<>();
//        changedAt.put(start, null);
//
//        // Keeps track of the shortest path we've found so far for every node
//        HashMap<Node<A>, Double> shortestPathMap = new HashMap<>();
//
//        // Setting every node's shortest path weight to positive infinity to start
//        // except the starting node, whose shortest path weight is 0
//        for (Node<A> node : nodes) {
//            if (node.equals(start))
//                shortestPathMap.put(start, 0.0);
//            else shortestPathMap.put(node, Double.POSITIVE_INFINITY);
//        }
//
//        // Now we go through all the nodes we can go to from the starting node
//        // (this keeps the loop a bit simpler)
//        for (Edge<A> edge : start.edges) {
//            shortestPathMap.put(edge.destination, edge.weight);
//            changedAt.put(edge.destination, start);
//        }
//
//        start.visit();
//
//        // This loop runs as long as there is an unvisited node that we can
//        // reach from any of the nodes we could till then
//        while (true) {
//            Node<A> currentNode = closestReachableUnvisited(shortestPathMap);
//            // If we haven't reached the end node yet, and there isn't another
//            // reachable node the path between start and end doesn't exist
//            // (they aren't connected)
//            if (currentNode == null) {
//                System.out.println("There isn't a path between " + start + " and " + end);
//                return List.of();
//            }
//
//            // If the closest non-visited node is our destination, we want to print the path
//            if (currentNode.equals(end)) {
//                System.out.println("The path with the smallest weight between "
//                        + start + " and " + end + " is:");
//
//                Node<A> child = end;
//
//                // It makes no sense to use StringBuilder, since
//                // repeatedly adding to the beginning of the string
//                // defeats the purpose of using StringBuilder
//                String path = end.toString();
//                result.add(end);
//                while (true) {
//                    Node<A> parent = changedAt.get(child);
//                    if (parent == null) {
//                        break;
//                    }
//
//                    // Since our changedAt map keeps track of child -> parent relations
//                    // in order to print the path we need to add the parent before the child and
//                    // it's descendants
//                    result.add(0, parent);
//                    path = parent + " " + path;
//                    child = parent;
//                }
//                System.out.println(path);
//                System.out.println("The path costs: " + shortestPathMap.get(end));
//                return result;
//            }
//            currentNode.visit();
//
//            // Now we go through all the unvisited nodes our current node has an edge to
//            // and check whether its shortest path value is better when going through our
//            // current node than whatever we had before
//            for (Edge<A> edge : currentNode.edges) {
//                if (edge.destination.isVisited())
//                    continue;
//
//                if (shortestPathMap.get(currentNode)
//                        + edge.weight
//                        < shortestPathMap.get(edge.destination)) {
//                    shortestPathMap.put(edge.destination,
//                            shortestPathMap.get(currentNode) + edge.weight);
//                    changedAt.put(edge.destination, currentNode);
//                }
//            }
//        }
        // We keep track of which path gives us the shortest path for each node
        // by keeping track how we arrived at a particular node, we effectively
        // keep a "pointer" to the parent node of each node, and we follow that
        // path to the start
        List<Node<A>> resultingPath = new LinkedList<>();
        HashMap<Node<A>, Node<A>> changedAt = new HashMap<>();
        changedAt.put(start, null);

        // Keeps track of the shortest path we've found so far for every node
        HashMap<Node<A>, Double> shortestPathMap = new HashMap<>();

        // Setting every node's shortest path weight to positive infinity to start
        // except the starting node, whose shortest path weight is 0
        for (Node<A> node : nodes) {
            if (node == start)
                shortestPathMap.put(start, 0.0);
            else shortestPathMap.put(node, Double.POSITIVE_INFINITY);
        }

        // Now we go through all the nodes we can go to from the starting node
        // (this keeps the loop a bit simpler)
        for (Edge<A> edge : start.edges) {
            shortestPathMap.put(edge.destination, edge.weight);
            changedAt.put(edge.destination, start);
        }

        start.visit();

        // This loop runs as long as there is an unvisited node that we can
        // reach from any of the nodes we could till then
        while (true) {
            Node<A> currentNode = closestReachableUnvisited(shortestPathMap);
            // If we haven't reached the end node yet, and there isn't another
            // reachable node the path between start and end doesn't exist
            // (they aren't connected)
            if (currentNode == null) {
                System.out.println("There isn't a path between " + start + " and " + end);
                return List.of();
            }

            // If the closest non-visited node is our destination, we want to print the path
            if (currentNode == end) {
                System.out.println("The path with the smallest weight between "
                        + start + " and " + end + " is:");
                resultingPath.add(0, end);
                Node<A> child = end;

                // It makes no sense to use StringBuilder, since
                // repeatedly adding to the beginning of the string
                // defeats the purpose of using StringBuilder
                String path = end.toString();
                while (true) {
                    Node<A> parent = changedAt.get(child);
                    if (parent == null) {
                        break;
                    }

                    // Since our changedAt map keeps track of child -> parent relations
                    // in order to print the path we need to add the parent before the child and
                    // it's descendants
                    resultingPath.add(0, parent);
                    path = parent + " " + path;
                    child = parent;
                }
                System.out.println(path);
                System.out.println("The path costs: " + shortestPathMap.get(end));
                return resultingPath;
            }
            currentNode.visit();

            // Now we go through all the unvisited nodes our current node has an edge to
            // and check whether its shortest path value is better when going through our
            // current node than whatever we had before
            for (Edge<A> edge : currentNode.edges) {
                if (edge.destination.isVisited())
                    continue;

                if (shortestPathMap.get(currentNode)
                        + edge.weight
                        < shortestPathMap.get(edge.destination)) {
                    shortestPathMap.put(edge.destination,
                            shortestPathMap.get(currentNode) + edge.weight);
                    changedAt.put(edge.destination, currentNode);
                }
            }
        }
    }

    private Node<A> closestReachableUnvisited(HashMap<Node<A>, Double> shortestPathMap) {

        double shortestDistance = Double.POSITIVE_INFINITY;
        Node<A> closestReachableNode = null;
        for (Node<A> node : nodes) {
            if (node.isVisited())
                continue;

            double currentDistance = shortestPathMap.get(node);
            if (currentDistance == Double.POSITIVE_INFINITY)
                continue;

            if (currentDistance < shortestDistance) {
                shortestDistance = currentDistance;
                closestReachableNode = node;
            }
        }
        return closestReachableNode;
    }
}