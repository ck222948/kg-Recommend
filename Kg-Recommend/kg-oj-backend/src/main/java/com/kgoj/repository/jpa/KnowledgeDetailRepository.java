package com.kgoj.repository.jpa;

import com.kgoj.domain.KnowledgeDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDetailRepository extends JpaRepository<KnowledgeDetail, Long> {
}