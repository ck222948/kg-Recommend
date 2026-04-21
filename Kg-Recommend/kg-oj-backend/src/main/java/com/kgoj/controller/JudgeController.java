package com.kgoj.controller;

import com.kgoj.domain.SubmitRequest;
import com.kgoj.judge.JudgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/judge")
public class JudgeController {

    @Autowired
    private JudgeService judgeService;

    @PostMapping("/submit")
    public String submitCode(@RequestBody SubmitRequest request) {
        return judgeService.judge(request.getQuestionId(), request.getCode());
    }
}