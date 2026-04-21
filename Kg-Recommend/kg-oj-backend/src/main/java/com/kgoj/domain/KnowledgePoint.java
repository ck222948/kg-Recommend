package com.kgoj.domain;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node("KnowledgePoint")
@Data
public class KnowledgePoint {

    @Id
    @org.springframework.data.neo4j.core.schema.GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Relationship(type = "PRE_REQUISITE", direction = Relationship.Direction.OUTGOING)
    private List<KnowledgePoint> preRequisites;

}