package com.betterbundle.sort.dag;

import java.util.*;

/**
 * 依赖图：边 u -> v 表示 u 必须先于 v 执行。
 */
public final class DependencyGraph {

    private final Set<Integer> nodes = new LinkedHashSet<>();
    private final Map<Integer, Set<Integer>> edges = new HashMap<>();

    public void addNode(int id) {
        nodes.add(id);
    }

    public void addEdge(int before, int after) {
        if (before == after) return;
        nodes.add(before);
        nodes.add(after);
        edges.computeIfAbsent(before, k -> new LinkedHashSet<>()).add(after);
    }

    /** 返回拓扑序；存在环时返回 null。 */
    public List<Integer> topoSort() {
        Map<Integer, Integer> indegree = new HashMap<>();
        for (int n : nodes) indegree.put(n, 0);
        for (Set<Integer> outs : edges.values()) {
            for (int v : outs) indegree.merge(v, 1, Integer::sum);
        }

        Deque<Integer> queue = new ArrayDeque<>();
        for (int n : nodes) {
            if (indegree.get(n) == 0) queue.add(n);
        }

        List<Integer> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            int u = queue.poll();
            order.add(u);
            Set<Integer> outs = edges.get(u);
            if (outs == null) continue;
            for (int v : outs) {
                int d = indegree.merge(v, -1, Integer::sum);
                if (d == 0) queue.add(v);
            }
        }
        return order.size() == nodes.size() ? order : null;
    }
}
