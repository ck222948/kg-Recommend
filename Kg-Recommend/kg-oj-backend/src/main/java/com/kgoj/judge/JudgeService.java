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

    public String judge(String questionIdStr, String userCode) {
        Long questionId = null;
        try {
            questionId = Long.parseLong(questionIdStr);
        } catch (Exception e) {
            // 如果解析失败，说明传入的可能是 Neo4j 的原生 elementId
            // 此时我们无法从 MySQL 中查询答案，只能退而求其次仅判断是否执行成功
            String actualOutput = codeSandboxService.executeJavaCode(userCode);
            if (actualOutput.startsWith("判题沙箱执行异常")) {
                return "⚠️ System Error (系统异常):\n" + actualOutput;
            }
            return "✅ Accepted (AC) 通过！\n(此题目暂无标准答案记录，仅校验执行通过)\n实际输出: " + actualOutput;
        }

        QuestionDetail question = questionDetailRepository.findById(questionId).orElse(null);
        if (question == null) {
             String actualOutput = codeSandboxService.executeJavaCode(userCode);
             if (actualOutput.startsWith("判题沙箱执行异常")) {
                 return "⚠️ System Error (系统异常):\n" + actualOutput;
             }
             return "✅ Accepted (AC) 通过！\n(此题目暂无标准答案记录，仅校验执行通过)\n实际输出: " + actualOutput;
        }

        String actualOutput = codeSandboxService.executeJavaCode(userCode);

        actualOutput = actualOutput != null ? actualOutput.trim() : "";
        String expectedOutput = question.getExpectedOutput() != null ? question.getExpectedOutput().trim() : "";

        if (actualOutput.startsWith("判题沙箱执行异常")) {
            return "⚠️ System Error (系统异常):\n" + actualOutput;
        } else if (actualOutput.equals(expectedOutput)) {
            return "✅ Accepted (AC) 通过！\n实际输出与标准答案完全一致: " + actualOutput;
        } else {
            return "❌ Wrong Answer (WA) 答案错误！\n期待输出: [" + expectedOutput + "]\n实际输出: [" + actualOutput + "]";
        }
    }
}