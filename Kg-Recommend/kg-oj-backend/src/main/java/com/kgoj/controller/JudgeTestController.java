package com.kgoj.controller;

import com.kgoj.judge.CodeSandboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sandbox")
public class JudgeTestController {

    @Autowired
    private CodeSandboxService codeSandboxService;

    @PostMapping("/run")
    public String runCode(@RequestBody String code) {
        return codeSandboxService.executeJavaCode(code);
    }
}