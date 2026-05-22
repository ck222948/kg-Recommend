package com.kgoj.controller;

import com.kgoj.domain.KnowledgeDetail;
import com.kgoj.repository.jpa.KnowledgeDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
            String cypher = "MATCH (kp) WHERE elementId(kp) = $id OR toString(id(kp)) = $id OR toString(kp.id) = $id " +
                            "OPTIONAL MATCH (kp)-[:PRE_REQUISITE]->(pre) " +
                            "OPTIONAL MATCH (ex:Exercise)-[:TESTS]->(kp) " +
                            "RETURN kp.name AS name, labels(kp)[0] AS type, kp.id AS detailId, " +
                            "collect(DISTINCT {id: elementId(pre), name: pre.name, type: labels(pre)[0]}) AS preKnowledges, " +
                            "collect(DISTINCT {id: elementId(ex), title: ex.title, difficulty: ex.difficulty, customId: ex.id}) AS relatedExercises";

            neo4jClient.query(cypher).bind(id).to("id").fetch().all().forEach(row -> {
                result.put("id", id);
                result.put("name", row.get("name"));
                result.put("type", row.get("type"));
                Object detailId = row.get("detailId");
                if (detailId != null) result.put("detailId", detailId);
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> preList = (List<Map<String, Object>>) row.get("preKnowledges");
                List<Map<String, Object>> cleanPreList = new ArrayList<>();
                for(Map<String, Object> m : preList) { if(m.get("id") != null && !m.get("id").toString().equals("null")) cleanPreList.add(m); }
                result.put("preKnowledges", cleanPreList);

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> exList = (List<Map<String, Object>>) row.get("relatedExercises");
                List<Map<String, Object>> cleanExList = new ArrayList<>();
                for(Map<String, Object> m : exList) { if(m.get("id") != null && !m.get("id").toString().equals("null")) cleanExList.add(m); }
                result.put("relatedExercises", cleanExList);
                
                putMysqlKnowledgeDetail(result, id, detailId, row.get("name"));
            });

            if (result.isEmpty()) {
                result.put("id", id);
                String decodedName = URLDecoder.decode(id, StandardCharsets.UTF_8);
                Optional<KnowledgeDetail> detailOpt = findKnowledgeDetailByName(decodedName);
                if (detailOpt.isPresent()) {
                    KnowledgeDetail detail = detailOpt.get();
                    result.put("name", detail.getName());
                    result.put("description", detail.getDescription());
                    if (detail.getVideoUrl() != null) result.put("videoUrl", detail.getVideoUrl());
                } else {
                    result.put("name", "获取信息失败");
                    result.put("description", "未能找到该知识点节点，请确认 Neo4j 中该节点仍然存在。");
                }
                result.put("preKnowledges", new ArrayList<>());
                result.put("relatedExercises", new ArrayList<>());
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Query Failed");
            return err;
        }
    }

    private void putMysqlKnowledgeDetail(Map<String, Object> result, String requestId, Object detailId, Object neoName) {
        Optional<KnowledgeDetail> detailOpt = Optional.empty();

        Long mysqlId = parseLong(detailId);
        if (mysqlId == null) mysqlId = parseLong(requestId);
        if (mysqlId != null) detailOpt = knowledgeDetailRepository.findById(mysqlId);

        if (detailOpt.isEmpty()) detailOpt = findKnowledgeDetailByName(neoName);

        if (detailOpt.isPresent()) {
            KnowledgeDetail detail = detailOpt.get();
            result.put("name", detail.getName());
            result.put("description", detail.getDescription());
            if (detail.getVideoUrl() != null) result.put("videoUrl", detail.getVideoUrl());
        } else {
            result.put("description", "系统暂无该节点的详细介绍 (" + neoName + ")。");
        }
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Optional<KnowledgeDetail> findKnowledgeDetailByName(Object nameValue) {
        if (nameValue == null) return Optional.empty();
        String name = nameValue.toString();
        Optional<KnowledgeDetail> exact = knowledgeDetailRepository.findFirstByName(name);
        if (exact.isPresent()) return exact;

        String normalizedName = normalizeName(name);
        return knowledgeDetailRepository.findAll().stream()
                .filter(detail -> normalizeName(detail.getName()).equals(normalizedName))
                .findFirst();
    }

    private String normalizeName(String value) {
        if (value == null) return "";
        return value
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }
}
