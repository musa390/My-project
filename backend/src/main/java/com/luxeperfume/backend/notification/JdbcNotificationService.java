package com.luxeperfume.backend.notification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/** [POL] JDBC implementation of the notification contract; event keys make writes idempotent. */
@Service
public class JdbcNotificationService implements NotificationService {
    private static final Set<String> ORDER_STATUSES =
            Set.of("PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED");

    private final JdbcTemplate jdbc;

    public JdbcNotificationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void orderPlaced(long userId, long orderId) {
        publish(userId, "order-placed:" + orderId, orderId,
                "notification.order.placed.title", "notification.order.placed.body");
    }

    @Override
    @Transactional
    public void orderCancelled(long userId, long orderId) {
        publish(userId, "order-cancelled:" + orderId, orderId,
                "notification.order.cancelled.title", "notification.order.cancelled.body");
    }

    @Override
    @Transactional
    public void orderStatusChanged(long userId, long orderId, String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!ORDER_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported order notification status");
        }
        publish(userId, "order-status:" + orderId + ":" + normalized, orderId,
                "notification.order.status.title", "notification.order.status." + normalized.toLowerCase(Locale.ROOT));
    }

    private void publish(long userId, String eventKey, long orderId, String titleKey, String messageKey) {
        if (userId <= 0 || orderId <= 0) throw new IllegalArgumentException("Notification owner and order IDs must be positive");
        jdbc.update("""
                INSERT IGNORE INTO user_notifications
                    (user_id,event_key,notification_type,aggregate_type,aggregate_id,title_key,message_key)
                VALUES(?,?,'ORDER','ORDER',?,?,?)
                """, userId, eventKey, orderId, titleKey, messageKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> inbox(long userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        long offset = (long) safePage * safeSize;
        return jdbc.queryForList("""
                SELECT id,notification_type,aggregate_type,aggregate_id,title_key,message_key,
                       is_read,created_at,read_at
                FROM user_notifications
                WHERE user_id=?
                ORDER BY created_at DESC,id DESC
                LIMIT ? OFFSET ?
                """, userId, safeSize, offset).stream().map(JdbcNotificationService::camelCase).toList();
    }

    @Override
    @Transactional
    public boolean markRead(long userId, long notificationId) {
        int changed = jdbc.update("""
                UPDATE user_notifications
                SET is_read=TRUE,read_at=COALESCE(read_at,CURRENT_TIMESTAMP)
                WHERE id=? AND user_id=?
                """, notificationId, userId);
        if (changed == 1) return true;
        Integer exists = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_notifications WHERE id=? AND user_id=?
                """, Integer.class, notificationId, userId);
        return exists != null && exists > 0;
    }

    private static Map<String, Object> camelCase(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach((key, value) -> {
            StringBuilder name = new StringBuilder();
            boolean upper = false;
            for (char character : key.toCharArray()) {
                if (character == '_') upper = true;
                else { name.append(upper ? Character.toUpperCase(character) : character); upper = false; }
            }
            result.put(name.toString(), value);
        });
        return result;
    }
}
