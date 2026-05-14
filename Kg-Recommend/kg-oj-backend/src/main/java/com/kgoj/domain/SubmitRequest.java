package com.kgoj.domain;

public class SubmitRequest {
    private String questionId;
    private String code;

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public Long getParsedQuestionId() {
        try {
            return Long.parseLong(questionId);
        } catch (Exception e) {
            return null;
        }
    }
}