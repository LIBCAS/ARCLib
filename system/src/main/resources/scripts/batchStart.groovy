package scripts

import cz.cas.lib.arclib.service.CoordinatorService

CoordinatorService coordinatorService = spring.getBean(CoordinatorService.class);
coordinatorService.processBatchOfSips(routineId);