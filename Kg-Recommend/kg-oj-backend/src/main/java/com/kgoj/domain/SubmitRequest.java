package com.kgoj.domain;

public class SubmitRequest {
    private Long questionId;
    private String code;

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}