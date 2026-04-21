package com.kgoj.controller;

import com.kgoj.domain.QuestionDetail;
import com.kgoj.repository.jpa.QuestionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/question")
public class QuestionController {

    @Autowired
    private QuestionDetailRepository questionDetailRepository;

    // 1. 添加题目详情信息（存入 MySQL）
    @PostMapping("/add")
    public String addQuestionDetail(@RequestBody QuestionDetail detail) {
        questionDetailRepository.save(detail);
        return "题目详情保存成功！";
    }

    // 2. 根据 ID 查询题目详情（从 MySQL 获取）
    @GetMapping("/{id}")
    public QuestionDetail getQuestionDetail(@PathVariable Long id) {
        Optional<QuestionDetail> optional = questionDetailRepository.findById(id);
        return optional.orElse(null);
    }
}