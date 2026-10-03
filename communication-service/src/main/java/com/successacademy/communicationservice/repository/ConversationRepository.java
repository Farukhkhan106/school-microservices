package com.successacademy.communicationservice.repository;

import com.successacademy.communicationservice.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByType(String type);

    Optional<Conversation> findFirstByTypeAndActiveTrue(String type);

    Optional<Conversation> findByTypeAndTargetClassAndTargetSection(String type, String targetClass, String targetSection);

    List<Conversation> findByTypeAndActiveTrue(String type);

    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant p1 ON c.id = p1.conversationId " +
           "JOIN ConversationParticipant p2 ON c.id = p2.conversationId " +
           "WHERE c.type = 'DIRECT' AND p1.userId = :u1 AND p2.userId = :u2")
    Optional<Conversation> findDirectConversationBetween(@Param("u1") Long u1, @Param("u2") Long u2);
}
