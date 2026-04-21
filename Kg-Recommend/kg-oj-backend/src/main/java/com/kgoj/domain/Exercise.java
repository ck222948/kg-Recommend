package com.kgoj.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Exercise")
@Data
public class Exercise {

    @Id
    @org.springframework.data.neo4j.core.schema.GeneratedValue
    private Long id;

    @Property("title")
    private String title;

    @Property("difficulty")
    private String difficulty;

}