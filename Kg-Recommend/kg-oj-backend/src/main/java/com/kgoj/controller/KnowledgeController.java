package com.kgoj.controller;

import com.kgoj.domain.KnowledgeDetail;
import com.kgoj.repository.jpa.KnowledgeDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Autowired
    private Neo4jClient neo4jClient;

    @Autowired
    private KnowledgeDetailRepository knowledgeDetailRepository;

    @PostMapping("/addDetail")
    public String addKnowledgeDetail(@RequestBody KnowledgeDetail detail) {
        knowledgeDetailRepository.save(detail);
        return "Success";
    }

    @GetMapping("/detail/{id}")
    public Map<String, Object> getKnowledgeDetail(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String cypher = "MATCH (kp) WHERE toString(id(kp)) = '" + id + "' OR toString(kp.id) = '" + id + "' " +
                            "OPTIONAL MATCH (kp)-[:PRE_REQUISITE]->(pre) " +
                            "OPTIONAL MATCH (ex:Exercise)-[:TESTS]->(kp) " +
                            "RETURN kp.name AS name, labels(kp)[0] AS type, " +
                            "collect(DISTINCT {id: toString(id(pre)), name: pre.name, type: labels(pre)[0]}) AS preKnowledges, " +
                            "collect(DISTINCT {id: toString(id(ex)), title: ex.title, difficulty: ex.difficulty}) AS relatedExercises";

            neo4jClient.query(cypher).fetch().all().forEach(row -> {
                result.put("id", id);
                result.put("name", row.get("name"));
                result.put("type", row.get("type"));
                
                List<Map<String, Object>> preList = (List<Map<String, Object>>) row.get("preKnowledges");
                List<Map<String, Object>> cleanPreList = new ArrayList<>();
                for(Map<String, Object> m : preList) { if(m.get("id") != null && !m.get("id").toString().equals("null")) cleanPreList.add(m); }
                result.put("preKnowledges", cleanPreList);

                List<Map<String, Object>> exList = (List<Map<String, Object>>) row.get("relatedExercises");
                List<Map<String, Object>> cleanExList = new ArrayList<>();
                for(Map<String, Object> m : exList) { if(m.get("id") != null && !m.get("id").toString().equals("null")) cleanExList.add(m); }
                result.put("relatedExercises", cleanExList);
                
                try {
                    Long numId = Long.parseLong(id);
                    Optional<KnowledgeDetail> detailOpt = knowledgeDetailRepository.findById(numId);
                    if (detailOpt.isPresent()) {
                        result.put("description", detailOpt.get().getDescription());
                        if (detailOpt.get().getVideoUrl() != null) {
                            result.put("videoUrl", detailOpt.get().getVideoUrl());
                        }
                    } else {
                        result.put("description", "系统暂无该节点的详细介绍 (" + row.get("name") + ")。");
                    }
                } catch (Exception parseEx) {
                    result.put("description", "ID格式不匹配");
                }
            });

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Query Failed");
            return err;
        }
    }
}