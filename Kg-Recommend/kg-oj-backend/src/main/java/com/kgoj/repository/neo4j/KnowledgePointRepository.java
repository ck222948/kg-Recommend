package com.kgoj.repository.neo4j;

import com.kgoj.domain.KnowledgePoint;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KnowledgePointRepository extends Neo4jRepository<KnowledgePoint, Long> {

    List<KnowledgePoint> findByName(String name);

}