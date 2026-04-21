package com.kgoj.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {

    @Autowired
    private Neo4jClient neo4jClient;

    @GetMapping("/analyze/{exerciseId}")
    public Map<String, Object> analyzeWeakness(@PathVariable Long exerciseId) {
        try {
            // 使用底层的 Neo4jClient 自己掌控查询和映射逻辑，规避 Spring Data 的 Map 转换 Bug
            String cypher = "MATCH (badEx:Exercise)-[:TESTS]->(weakKp:KnowledgePoint) " +
                            "WHERE toString(badEx.id) = '" + exerciseId + "' OR toString(id(badEx)) = '" + exerciseId + "' " +
                            "OPTIONAL MATCH (weakKp)-[:PRE_REQUISITE]->(preKp:KnowledgePoint)<-[:TESTS]-(easyEx:Exercise) " +
                            "RETURN weakKp.name AS weakPoint, preKp.name AS recommendKnowledge, toString(id(easyEx)) AS recommendExerciseId, easyEx.title AS recommendExerciseTitle " +
                            "LIMIT 1";

            Map<String, Object> result = new HashMap<>();
            
            neo4jClient.query(cypher).fetch().all().forEach(row -> {
                result.put("weakPoint", row.get("weakPoint"));
                result.put("recommendKnowledge", row.get("recommendKnowledge"));
                result.put("recommendExerciseId", row.get("recommendExerciseId"));
                result.put("recommendExerciseTitle", row.get("recommendExerciseTitle"));
            });

            if (result.isEmpty() || result.get("weakPoint") == null) {
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("message", "未能找到明确的前置依赖，建议先复习 Java 基础。");
                return fallback;
            }
            return result;
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("error", "诊断失败");
            err.put("msg", e.getMessage());
            return err;
        }
    }
}