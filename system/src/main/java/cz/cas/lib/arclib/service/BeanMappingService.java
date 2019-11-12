package cz.cas.lib.arclib.service;

import org.dozer.Mapper;

import java.util.Collection;
import java.util.List;

public interface BeanMappingService {

    public <T> List<T> mapTo(Collection<?> objects, Class<T> mapToClass);

    public <T> T mapTo(Object u, Class<T> mapToClass);

    public Mapper getMapper();
}
