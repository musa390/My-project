package com.luxeperfume.backend.notification;

import java.util.List;
import java.util.Map;

/** [ABS] Application contract for transactional in-app notifications. */
public interface NotificationService {
    void orderPlaced(long userId, long orderId);
    void orderCancelled(long userId, long orderId);
    void orderStatusChanged(long userId, long orderId, String status);
    List<Map<String, Object>> inbox(long userId, int page, int size);
    boolean markRead(long userId, long notificationId);
}
