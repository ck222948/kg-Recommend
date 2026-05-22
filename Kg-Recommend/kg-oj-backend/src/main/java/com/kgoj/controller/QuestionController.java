package com.kgoj.controller;

import com.kgoj.domain.QuestionDetail;
import com.kgoj.repository.jpa.QuestionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    // 2. 根据 ID 查询题目详情（从 MySQL 获取）；如果不是数字，则按标题兜底查询
    @GetMapping("/{id}")
    public QuestionDetail getQuestionDetail(@PathVariable String id) {
        Optional<QuestionDetail> optional = Optional.empty();
        try {
            optional = questionDetailRepository.findById(Long.parseLong(id));
        } catch (NumberFormatException ignored) {
            String title = URLDecoder.decode(id, StandardCharsets.UTF_8);
            optional = questionDetailRepository.findFirstByTitle(title);
        }
        return optional.orElse(null);
    }
}
