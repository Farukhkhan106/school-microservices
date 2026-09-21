package com.successacademy.communicationservice.repository;

import com.successacademy.communicationservice.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversationId = :conversationId AND (:lastReadId IS NULL OR m.id > :lastReadId) AND m.senderId <> :userId")
    long countUnreadMessages(@Param("conversationId") Long conversationId,
                             @Param("lastReadId") Long lastReadId,
                             @Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.createdAt DESC")
    List<Message> searchMessages(@Param("conversationId") Long conversationId, @Param("query") String query);
}
