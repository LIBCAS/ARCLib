package cz.inqool.uas.rest;

import cz.inqool.uas.domain.DomainObject;
import cz.inqool.uas.index.dto.Params;
import cz.inqool.uas.store.HistoricStore;
import cz.inqool.uas.store.Transactional;
import cz.inqool.uas.store.revision.Revision;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

public interface HistoricApi<T extends DomainObject, U extends HistoricStore<T>> {
    U getHistoricStore();

    @Transactional
    @ApiOperation(value = "Lists all revisions of the specified instance.",
            response = List.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = List.class)})
    @RequestMapping(value = "/{id}/revisions", method = RequestMethod.GET)
    default List<Revision> revisions(@ApiParam(value = "Id of the instance", required = true)
                                         @PathVariable("id") String id,
                                     @ApiParam(value = "Parameters to comply with", required = true)
                                     @ModelAttribute Params params) {
        U store = getHistoricStore();
        return store.getRevisions(id, params);
    }

    @Transactional
    @ApiOperation(value = "Get specific revision of the given instance.",
            response = DomainObject.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful response", response = DomainObject.class)})
    @RequestMapping(value = "/{id}/revisions/{revisionId}", method = RequestMethod.GET)
    default T revision(@ApiParam(value = "Id of the instance", required = true)
                           @PathVariable("id") String id,
                       @ApiParam(value = "Id of the revision", required = true)
                           @PathVariable("revisionId") long revisionId) {
        U store = getHistoricStore();
        return store.findAtRevision(id, revisionId);
    }
}
