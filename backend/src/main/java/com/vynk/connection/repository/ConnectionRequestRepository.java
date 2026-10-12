package com.vynk.connection.repository;

import com.vynk.connection.entity.ConnectionRequest;
import com.vynk.connection.entity.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, Long> {

    long countBySenderIdAndStatus(Long senderId, ConnectionStatus status);

    @Query("SELECT COUNT(c) > 0 FROM ConnectionRequest c WHERE ((c.sender.id = :u1 AND c.receiver.id = :u2) OR (c.sender.id = :u2 AND c.receiver.id = :u1)) AND c.status = 'PENDING'")
    boolean existsPendingBetween(@Param("u1") Long u1, @Param("u2") Long u2);

    @Query("SELECT COUNT(c) > 0 FROM ConnectionRequest c WHERE ((c.sender.id = :u1 AND c.receiver.id = :u2) OR (c.sender.id = :u2 AND c.receiver.id = :u1)) AND c.status = 'ACCEPTED'")
    boolean existsAcceptedBetween(@Param("u1") Long u1, @Param("u2") Long u2);

    @Query("SELECT c FROM ConnectionRequest c WHERE ((c.sender.id = :u1 AND c.receiver.id = :u2) OR (c.sender.id = :u2 AND c.receiver.id = :u1)) AND c.status = 'REJECTED' AND c.updatedAt > :since ORDER BY c.updatedAt DESC")
    List<ConnectionRequest> findRecentRejectedBetween(@Param("u1") Long u1, @Param("u2") Long u2, @Param("since") LocalDateTime since);

    @Query("SELECT c FROM ConnectionRequest c WHERE (c.sender.id = :userId OR c.receiver.id = :userId) AND c.status = 'ACCEPTED' ORDER BY c.updatedAt DESC")
    List<ConnectionRequest> findAllAcceptedForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM ConnectionRequest c WHERE c.receiver.id = :userId AND c.status = 'PENDING' ORDER BY c.createdAt DESC")
    List<ConnectionRequest> findPendingIncoming(@Param("userId") Long userId);

    @Query("SELECT c FROM ConnectionRequest c WHERE c.sender.id = :userId AND c.status = 'PENDING' ORDER BY c.createdAt DESC")
    List<ConnectionRequest> findPendingOutgoing(@Param("userId") Long userId);
}
