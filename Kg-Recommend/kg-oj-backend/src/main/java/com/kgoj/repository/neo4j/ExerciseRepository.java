package com.kgoj.repository.neo4j;

import com.kgoj.domain.Exercise;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExerciseRepository extends Neo4jRepository<Exercise, Long> {

    @Query("MATCH (at:AbilityTarget {name: $abilityName})-[:REQUIRES]->(kp:KnowledgePoint)<-[:TESTS]-(ex:Exercise) RETURN DISTINCT ex ORDER BY CASE ex.difficulty WHEN 'Easy' THEN 1 WHEN 'Medium' THEN 2 WHEN 'Hard' THEN 3 END")
    List<Exercise> recommendByAbility(@Param("abilityName") String abilityName);
}