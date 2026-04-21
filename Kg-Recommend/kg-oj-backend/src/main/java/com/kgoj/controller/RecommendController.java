package com.kgoj.controller;

import com.kgoj.domain.Exercise;
import com.kgoj.repository.neo4j.ExerciseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Autowired
    private ExerciseRepository exerciseRepository;

    // 暴露一个 HTTP GET 接口给前端调用
    @GetMapping("/by-ability")
    public List<Exercise> getRecommendations(@RequestParam String abilityName) {
        // 调用 Repository 里写好的 Cypher 语句去图数据库查询
        return exerciseRepository.recommendByAbility(abilityName);
    }
}