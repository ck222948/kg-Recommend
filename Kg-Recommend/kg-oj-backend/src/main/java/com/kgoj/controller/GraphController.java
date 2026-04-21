package com.kgoj.controller;

import com.kgoj.domain.GraphDataDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    @Autowired
    private Neo4jClient neo4jClient;

    // 获取用于前端渲染的完整图谱数据
    @GetMapping("/all")
    public GraphDataDTO getAllGraphData() {
        GraphDataDTO graphData = new GraphDataDTO();
        List<GraphDataDTO.NodeDTO> nodes = new ArrayList<>();
        List<GraphDataDTO.EdgeDTO> edges = new ArrayList<>();

        // 查询所有的节点和关系 (限制 100 条以防图太大卡死)
        String cypher = "MATCH (n)-[r]->(m) RETURN n, r, m LIMIT 100";
        
        neo4jClient.query(cypher).fetch().all().forEach(row -> {
            // 解析源节点
            org.neo4j.driver.types.Node sourceNode = (org.neo4j.driver.types.Node) row.get("n");
            String sourceId = String.valueOf(sourceNode.id());
            String sourceLabel = sourceNode.get("name").isNull() ? 
                                (sourceNode.get("title").isNull() ? "Unknown" : sourceNode.get("title").asString()) : 
                                sourceNode.get("name").asString();
            String sourceType = sourceNode.labels().iterator().next();

            // 解析目标节点
            org.neo4j.driver.types.Node targetNode = (org.neo4j.driver.types.Node) row.get("m");
            String targetId = String.valueOf(targetNode.id());
            String targetLabel = targetNode.get("name").isNull() ? 
                                (targetNode.get("title").isNull() ? "Unknown" : targetNode.get("title").asString()) : 
                                targetNode.get("name").asString();
            String targetType = targetNode.labels().iterator().next();

            // 解析关系
            org.neo4j.driver.types.Relationship relationship = (org.neo4j.driver.types.Relationship) row.get("r");
            String relType = relationship.type();

            // 将解析后的数据加入到返回列表中 (前端会用 Set 去重，这里为了简单直接加)
            nodes.add(new GraphDataDTO.NodeDTO(sourceId, sourceLabel, sourceType));
            nodes.add(new GraphDataDTO.NodeDTO(targetId, targetLabel, targetType));
            edges.add(new GraphDataDTO.EdgeDTO(sourceId, targetId, relType));
        });

        graphData.setNodes(nodes);
        graphData.setEdges(edges);
        return graphData;
    }
}