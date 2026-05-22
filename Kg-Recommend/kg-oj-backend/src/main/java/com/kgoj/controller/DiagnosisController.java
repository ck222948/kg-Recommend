package com.kgoj.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {

    @Autowired
    private Neo4jClient neo4jClient;

    @GetMapping("/tested-concepts/{exerciseId}")
    public List<Map<String, Object>> getTestedConcepts(@PathVariable String exerciseId) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String cypher = "MATCH (ex:Exercise)-[r:TESTS]->(kp) " +
                            "WHERE elementId(ex) = '" + exerciseId + "' OR toString(ex.id) = '" + exerciseId + "' " +
                            "RETURN elementId(kp) AS id, kp.name AS name, r.weight AS weight " +
                            "ORDER BY r.weight DESC";

            neo4jClient.query(cypher).fetch().all().forEach(row -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", row.get("id"));
                map.put("name", row.get("name"));
                map.put("weight", row.get("weight") != null ? row.get("weight") : 0);
                result.add(map);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @GetMapping("/recommend")
    public Map<String, Object> getRecommendation(@RequestParam String conceptId, 
                                                 @RequestParam String conceptName,
                                                 @RequestParam(required = false, defaultValue = "") String currentExerciseId) {
        Map<String, Object> result = new HashMap<>();
        result.put("weakPoint", conceptName);

        try {
            String exCypher = "MATCH (kp) WHERE elementId(kp) = '" + conceptId + "' OR toString(kp.id) = '" + conceptId + "' " +
                              "MATCH (ex:Exercise)-[:TESTS]->(kp) " +
                              "WHERE elementId(ex) <> '" + currentExerciseId + "' AND toString(ex.id) <> '" + currentExerciseId + "' " +
                              "RETURN kp.name AS kpName, elementId(ex) AS exId, ex.title AS exTitle, ex.id AS customExId " +
                              "ORDER BY CASE ex.difficulty WHEN 'Easy' THEN 1 WHEN 'Normal' THEN 2 WHEN 'Hard' THEN 3 ELSE 4 END " +
                              "LIMIT 1";

            boolean foundExercise = false;
            for (Map<String, Object> row : neo4jClient.query(exCypher).fetch().all()) {
                result.put("recommendType", "Exercise");
                result.put("recommendKnowledge", row.get("kpName"));
                result.put("recommendExerciseId", row.get("exId"));
                result.put("recommendExerciseCustomId", row.get("customExId"));
                result.put("recommendExerciseTitle", row.get("exTitle"));
                foundExercise = true;
                break;
            }

            if (!foundExercise) {
                // 如果找不到低级别题，找有没有对应的教程可以退回
                String tutCypher = "MATCH (tut)-[:EXPLAINS]->(kp) " +
                                   "WHERE (tut:Video OR tut:Article) " +
                                   "AND (elementId(kp) = '" + conceptId + "' OR toString(kp.id) = '" + conceptId + "') " +
                                   "RETURN elementId(tut) AS tutId, tut.title AS tutTitle, tut.url AS tutUrl LIMIT 1";
                
                boolean foundTutorial = false;
                for (Map<String, Object> row : neo4jClient.query(tutCypher).fetch().all()) {
                    result.put("recommendType", "Resource");
                    result.put("tutorialId", row.get("tutId"));
                    result.put("tutorialTitle", row.get("tutTitle"));
                    result.put("tutorialUrl", row.get("tutUrl"));
                    foundTutorial = true;
                    break;
                }
                
                // 【核心修复】 如果连教程也没有，那就只推荐学习该知识点本身
                if (!foundTutorial) {
                    result.put("recommendType", "Knowledge");
                    result.put("recommendKnowledgeId", conceptId);
                    result.put("recommendKnowledgeName", conceptName);
                }
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", "诊断失败: " + e.getMessage());
            return result;
        }
    }
}
