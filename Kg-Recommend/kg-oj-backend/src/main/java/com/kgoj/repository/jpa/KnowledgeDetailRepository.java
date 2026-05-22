package com.kgoj.repository.jpa;

import com.kgoj.domain.KnowledgeDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KnowledgeDetailRepository extends JpaRepository<KnowledgeDetail, Long> {
    Optional<KnowledgeDetail> findFirstByName(String name);
}
