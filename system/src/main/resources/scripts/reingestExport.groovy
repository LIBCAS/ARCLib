package scripts

import cz.cas.lib.arclib.service.ReingestService

ReingestService reingestService = spring.getBean(ReingestService.class)
reingestService.exportReingestBatch()