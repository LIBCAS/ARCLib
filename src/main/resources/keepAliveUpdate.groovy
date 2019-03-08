import cz.cas.lib.arclib.service.AipService

AipService aipService = spring.getBean(AipService.class)
aipService.testAndCancelXmlUpdate(authorialPackageId)