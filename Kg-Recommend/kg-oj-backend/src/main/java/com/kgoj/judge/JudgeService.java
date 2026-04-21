package com.kgoj.judge;

import com.kgoj.domain.QuestionDetail;
import com.kgoj.repository.jpa.QuestionDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JudgeService {

    @Autowired
    private QuestionDetailRepository questionDetailRepository;

    @Autowired
    private CodeSandboxService codeSandboxService;

    public String judge(Long questionId, String userCode) {
        // 1. 从 MySQL 查询这道题的标准答案
        QuestionDetail question = questionDetailRepository.findById(questionId).orElse(null);
        if (question == null) {
            return "❌ 判题失败：找不到题目ID " + questionId;
        }

        // 2. 调用沙箱执行用户的代码，拿到实际输出
        String actualOutput = codeSandboxService.executeJavaCode(userCode);

        // 去除首尾的多余空格和换行符，防止因为多了一个回车导致误判
        actualOutput = actualOutput != null ? actualOutput.trim() : "";
        String expectedOutput = question.getExpectedOutput() != null ? question.getExpectedOutput().trim() : "";

        // 3. 核心判题逻辑（比对）
        if (actualOutput.startsWith("判题沙箱执行异常")) {
            return "⚠️ System Error (系统异常):\n" + actualOutput;
        } else if (actualOutput.equals(expectedOutput)) {
            return "✅ Accepted (AC) 通过！\n实际输出与标准答案完全一致：" + actualOutput;
        } else {
            return "❌ Wrong Answer (WA) 答案错误！\n期望输出: [" + expectedOutput + "]\n实际输出: [" + actualOutput + "]";
        }
    }
}