package com.kgoj.repository.jpa;
import com.kgoj.domain.QuestionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface QuestionDetailRepository extends JpaRepository<QuestionDetail, Long> {
    Optional<QuestionDetail> findFirstByTitle(String title);
}
