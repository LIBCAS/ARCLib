package scripts

import cz.cas.lib.arclib.service.NotificationService

NotificationService service = spring.getBean(NotificationService.class)
service.sendFormatsRevisionNotification(notificationId)