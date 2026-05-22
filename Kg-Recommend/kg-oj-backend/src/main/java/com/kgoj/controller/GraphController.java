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
    public Map<String, Object> getGraphData(@RequestParam(required = false, defaultValue = "ALL") String module) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        String normalizedModule = module == null || module.isBlank() ? "ALL" : module;

        if ("LOBBY".equalsIgnoreCase(normalizedModule)) {
            queryLobby(nodes);
        } else if ("ALL".equalsIgnoreCase(normalizedModule)) {
            queryAllGraph(nodes, edges);
        } else {
            queryModuleGraph(normalizedModule, nodes, edges);
        }

        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }

    private void queryLobby(List<Map<String, Object>> nodes) {
        String cypher = "MATCH (n:MacroConcept) RETURN n ORDER BY coalesce(n.order, 999), n.name LIMIT 50";

        neo4jClient.query(cypher).fetch().all().forEach(row -> {
            org.neo4j.driver.types.Node node = (org.neo4j.driver.types.Node) row.get("n");
            if (node != null) addNode(nodes, node);
        });
    }

    private void queryAllGraph(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        String cypher = "MATCH (sourceNode) " +
                "OPTIONAL MATCH (sourceNode)-[rel]->(targetNode) " +
                "RETURN sourceNode, rel, targetNode LIMIT 800";

        addGraphRows(cypher, null, nodes, edges);
    }

    private void queryModuleGraph(String module, List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        String cypher = "MATCH (macro:MacroConcept {name: $module}) " +
                "OPTIONAL MATCH (micro)-[:BELONGS_TO]->(macro) " +
                "OPTIONAL MATCH (micro)-[:PRE_REQUISITE]->(pre) " +
                "OPTIONAL MATCH (ex:Exercise)-[:TESTS]->(micro) " +
                "OPTIONAL MATCH (res)-[:EXPLAINS]->(micro) " +
                "WHERE res:Video OR res:Article " +
                "WITH collect(DISTINCT macro) + collect(DISTINCT micro) + collect(DISTINCT pre) + collect(DISTINCT ex) + collect(DISTINCT res) AS rawNodes " +
                "UNWIND rawNodes AS sourceNode " +
                "WITH collect(DISTINCT sourceNode) AS graphNodes " +
                "UNWIND graphNodes AS sourceNode " +
                "WITH graphNodes, sourceNode WHERE sourceNode IS NOT NULL " +
                "OPTIONAL MATCH (sourceNode)-[rel]->(targetNode) " +
                "WHERE targetNode IN graphNodes " +
                "RETURN sourceNode, rel, targetNode LIMIT 500";

        addGraphRows(cypher, module, nodes, edges);
    }

    private void addGraphRows(String cypher, String module,
                              List<Map<String, Object>> nodes,
                              List<Map<String, Object>> edges) {
        if (module != null) {
            neo4jClient.query(cypher)
                    .bind(module).to("module")
                    .fetch()
                    .all()
                    .forEach(row -> addGraphRow(row, nodes, edges));
            return;
        }

        neo4jClient.query(cypher)
                .fetch()
                .all()
                .forEach(row -> addGraphRow(row, nodes, edges));
    }

    private void addGraphRow(Map<String, Object> row,
                             List<Map<String, Object>> nodes,
                             List<Map<String, Object>> edges) {
        org.neo4j.driver.types.Node sourceNode = (org.neo4j.driver.types.Node) row.get("sourceNode");
        org.neo4j.driver.types.Node targetNode = (org.neo4j.driver.types.Node) row.get("targetNode");
        org.neo4j.driver.types.Relationship rel = (org.neo4j.driver.types.Relationship) row.get("rel");

        if (sourceNode != null) addNode(nodes, sourceNode);
        if (targetNode != null) addNode(nodes, targetNode);
        if (rel != null) addEdge(edges, rel);
    }

    private void addNode(List<Map<String, Object>> nodes, org.neo4j.driver.types.Node neoNode) {
        String id = neoNode.elementId();
        if (nodes.stream().anyMatch(n -> n.get("id").equals(id))) return;
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        String nodeType = neoNode.labels().iterator().next();
        for (String candidate : List.of("MacroConcept", "MicroConcept", "KnowledgePoint", "Exercise", "Video", "Article")) {
            if (neoNode.hasLabel(candidate)) {
                nodeType = candidate;
                break;
            }
        }
        map.put("type", nodeType);
        
        if (!neoNode.get("name").isNull()) map.put("label", neoNode.get("name").asString());
        else if (!neoNode.get("title").isNull()) map.put("label", neoNode.get("title").asString());
        else map.put("label", "Unknown");

        if (!neoNode.get("difficulty").isNull()) map.put("difficulty", neoNode.get("difficulty").asString());
        if (!neoNode.get("url").isNull()) map.put("url", neoNode.get("url").asString());
        if (!neoNode.get("id").isNull()) {
            Object businessId = neoNode.get("id").asObject();
            map.put("detailId", businessId);
            map.put("exId", businessId);
        }

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
