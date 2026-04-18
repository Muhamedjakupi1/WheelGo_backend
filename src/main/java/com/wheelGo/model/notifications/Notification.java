package com.wheelGo.model.notifications;

import com.wheelGo.model.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table (name = "notifications")
@Getter @Setter
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column (name = "user_id", nullable = false)
    private UUID userId;

    @Column (name = "type", nullable = false, length = 50)
    private String type;

    @Column (name = "title" , nullable = false, length = 150)
    private String title;

    @Column (name = "body", nullable = false)
    private String body;

    @Column (name = "channel", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    private NotificationChannel channel = NotificationChannel.EMAIL;

    @Column (name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "sent-at", nullable = false, updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

}
