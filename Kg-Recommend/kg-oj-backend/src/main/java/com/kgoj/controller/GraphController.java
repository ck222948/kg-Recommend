package com.kgoj.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    @Autowired
    private Neo4jClient neo4jClient;

    @GetMapping("/all")
    public Map<String, Object> getGraphData(@RequestParam(required = false) String module) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        String cypher;
        if (module != null && !module.isEmpty() && !module.equals("ALL") && !module.equals("LOBBY")) {
            // 子图模式：查询特定大类，及其相关的微观知识点、题目和教程。包括它们互相之间的所有关系。
            cypher = "MATCH (macro:MacroConcept {name: '" + module + "'}) " +
                     "OPTIONAL MATCH (micro:MicroConcept)-[r_belong:BELONGS_TO]->(macro) " +
                     "OPTIONAL MATCH (micro)-[r_pre:PRE_REQUISITE]->(pre:MicroConcept) " +
                     "OPTIONAL MATCH (ex:Exercise)-[r_test:TESTS]->(micro) " +
                     "OPTIONAL MATCH (tut:Tutorial)-[r_exp:EXPLAINS]->(micro) " +
                     "RETURN macro, micro, pre, ex, tut, r_belong, r_pre, r_test, r_exp LIMIT 300";
        } else if ("ALL".equals(module)) {
            // 上帝视角：查询图数据库里的一切点和一切边！
            cypher = "MATCH (n)-[r]->(m) RETURN n AS sourceNode, r AS rel, m AS targetNode LIMIT 500";
            
            neo4jClient.query(cypher).fetch().all().forEach(row -> {
                org.neo4j.driver.types.Node sourceNode = (org.neo4j.driver.types.Node) row.get("sourceNode");
                org.neo4j.driver.types.Node targetNode = (org.neo4j.driver.types.Node) row.get("targetNode");
                org.neo4j.driver.types.Relationship rel = (org.neo4j.driver.types.Relationship) row.get("rel");
                
                if (sourceNode != null) addNode(nodes, sourceNode);
                if (targetNode != null) addNode(nodes, targetNode);
                if (rel != null) addEdge(edges, rel);
            });
            
            result.put("nodes", nodes);
            result.put("edges", edges);
            return result;
            
        } else {
            // LOBBY 模式：只查询大厅入口卡片 (只查 MacroConcept)
            cypher = "MATCH (n:MacroConcept) RETURN n LIMIT 20";
            
            neo4jClient.query(cypher).fetch().all().forEach(row -> {
                org.neo4j.driver.types.Node node = (org.neo4j.driver.types.Node) row.get("n");
                if (node != null) addNode(nodes, node);
            });
            result.put("nodes", nodes);
            result.put("edges", edges);
            return result;
        }

        // 解析子图模式的复杂行
        neo4jClient.query(cypher).fetch().all().forEach(row -> {
            org.neo4j.driver.types.Node macro = (org.neo4j.driver.types.Node) row.get("macro");
            if (macro != null) addNode(nodes, macro);

            org.neo4j.driver.types.Node micro = (org.neo4j.driver.types.Node) row.get("micro");
            if (micro != null) addNode(nodes, micro);
            
            org.neo4j.driver.types.Node pre = (org.neo4j.driver.types.Node) row.get("pre");
            if (pre != null) addNode(nodes, pre);
            
            org.neo4j.driver.types.Node ex = (org.neo4j.driver.types.Node) row.get("ex");
            if (ex != null) addNode(nodes, ex);
            
            org.neo4j.driver.types.Node tut = (org.neo4j.driver.types.Node) row.get("tut");
            if (tut != null) addNode(nodes, tut);

            // 解析边
            addEdge(edges, row.get("r_belong"));
            addEdge(edges, row.get("r_pre"));
            addEdge(edges, row.get("r_test"));
            addEdge(edges, row.get("r_exp"));
        });

        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    private void addNode(List<Map<String, Object>> nodes, org.neo4j.driver.types.Node neoNode) {
        String id = neoNode.elementId();
        if (nodes.stream().anyMatch(n -> n.get("id").equals(id))) return;
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("type", neoNode.labels().iterator().next());
        
        if (!neoNode.get("name").isNull()) map.put("label", neoNode.get("name").asString());
        else if (!neoNode.get("title").isNull()) map.put("label", neoNode.get("title").asString());
        else map.put("label", "Unknown");

        if (!neoNode.get("difficulty").isNull()) map.put("difficulty", neoNode.get("difficulty").asString());
        if (!neoNode.get("url").isNull()) map.put("url", neoNode.get("url").asString());
        if (!neoNode.get("id").isNull()) map.put("exId", neoNode.get("id").asLong());

        nodes.add(map);
    }

    private void addEdge(List<Map<String, Object>> edges, Object relObj) {
        if (relObj == null) return;
        org.neo4j.driver.types.Relationship rel = (org.neo4j.driver.types.Relationship) relObj;
        String source = rel.startNodeElementId();
        String target = rel.endNodeElementId();
        String type = rel.type();
        
        if (edges.stream().anyMatch(e -> e.get("source").equals(source) && e.get("target").equals(target) && e.get("label").equals(type))) return;

        Map<String, Object> map = new HashMap<>();
        map.put("source", source);
        map.put("target", target);
        map.put("label", type);
        
        if (!rel.get("weight").isNull()) {
            map.put("weight", rel.get("weight").asInt());
            map.put("label", type + "(" + rel.get("weight").asInt() + "%)");
        }
        
        edges.add(map);
    }
}