package com.vynk.message.repository;

import com.vynk.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m " +
           "WHERE (m.sender.id = :u1 AND m.recipient.id = :u2) " +
           "   OR (m.sender.id = :u2 AND m.recipient.id = :u1) " +
           "ORDER BY m.createdAt DESC")
    Page<Message> findConversationHistory(
            @Param("u1") Long u1,
            @Param("u2") Long u2,
            Pageable pageable
    );

    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.recipient.id = :recipientId AND m.sender.id = :senderId AND m.readAt IS NULL")
    long countUnreadFromSender(
            @Param("recipientId") Long recipientId,
            @Param("senderId") Long senderId
    );

    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.recipient.id = :recipientId AND m.readAt IS NULL")
    long countTotalUnread(@Param("recipientId") Long recipientId);

    @Query("SELECT m.sender.id, COUNT(m) FROM Message m " +
           "WHERE m.recipient.id = :recipientId AND m.readAt IS NULL " +
           "GROUP BY m.sender.id")
    List<Object[]> countUnreadGroupedBySender(@Param("recipientId") Long recipientId);

    @Query("SELECT DISTINCT CASE WHEN m.sender.id = :userId THEN m.recipient.id ELSE m.sender.id END " +
           "FROM Message m " +
           "WHERE m.sender.id = :userId OR m.recipient.id = :userId")
    List<Long> findDistinctConversationPartnerIds(@Param("userId") Long userId);

    @Query(value = "SELECT * FROM messages m " +
                   "WHERE (m.sender_id = :u1 AND m.recipient_id = :u2) " +
                   "   OR (m.sender_id = :u2 AND m.recipient_id = :u1) " +
                   "ORDER BY m.created_at DESC LIMIT 1", nativeQuery = true)
    Message findLatestMessageBetween(@Param("u1") Long u1, @Param("u2") Long u2);

    @Query("SELECT m FROM Message m " +
           "JOIN FETCH m.sender " +
           "JOIN FETCH m.recipient " +
           "WHERE m.id IN (" +
           "  SELECT MAX(m2.id) FROM Message m2 " +
           "  WHERE (m2.sender.id = :myId AND m2.recipient.id IN :peerIds) " +
           "     OR (m2.recipient.id = :myId AND m2.sender.id IN :peerIds) " +
           "  GROUP BY CASE WHEN m2.sender.id = :myId THEN m2.recipient.id ELSE m2.sender.id END" +
           ")")
    List<Message> findLatestMessagesForPeers(
            @Param("myId") Long myId,
            @Param("peerIds") java.util.Collection<Long> peerIds
    );

    @Modifying
    @Query("UPDATE Message m SET m.readAt = :readAt " +
           "WHERE m.recipient.id = :recipientId AND m.sender.id = :senderId AND m.readAt IS NULL")
    int markConversationAsRead(
            @Param("recipientId") Long recipientId,
            @Param("senderId") Long senderId,
            @Param("readAt") LocalDateTime readAt
    );
}
