package org.cbioportal.web;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import springfox.documentation.annotations.ApiIgnore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;

import org.cbioportal.model.GenericAssayData;
import org.cbioportal.model.meta.GenericAssayMeta;
import org.cbioportal.service.GenericAssayService;
import org.cbioportal.service.exception.GenericAssayNotFoundException;
import org.cbioportal.service.exception.MolecularProfileNotFoundException;
import org.cbioportal.web.config.annotation.PublicApi;
import org.cbioportal.web.parameter.GenericAssayDataFilter;
import org.cbioportal.web.parameter.HeaderKeyConstants;
import org.cbioportal.web.parameter.Projection;
import org.cbioportal.web.parameter.GenericAssayDataMultipleStudyFilter;
import org.cbioportal.web.parameter.SampleMolecularIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PublicApi
@RestController
@Validated
@Api(tags = "P. Generic Assay", description = " ")
public class GenericAssayController {
    
    @Autowired
    private GenericAssayService genericAssayService;

    @RequestMapping(value = "/generic_assay_meta/{stableId}", method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Get all meta data in generic-assay by stable id")
    public ResponseEntity<GenericAssayMeta> getAllGenericAssayMetaData(
        @ApiParam(required = true, value = "Generic Assay Stable ID")
        @PathVariable String stableId) throws GenericAssayNotFoundException {
            GenericAssayMeta result = genericAssayService.getGenericAssayMetaByStableId(stableId);
            return new ResponseEntity<>(result, HttpStatus.OK);
    }


    @RequestMapping(value = "/generic_assay_meta/fetch", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Fetch meta data for generic-assay by ID")
    public ResponseEntity<List<GenericAssayMeta>> fetchAllGenericAssayMetaData(
        @RequestBody List<String> stableIds) throws GenericAssayNotFoundException {
            List<GenericAssayMeta> result = genericAssayService.getGenericAssayMetaByStableIds(stableIds);
            return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PreAuthorize("hasPermission(#molecularProfileId, 'MolecularProfileId', 'read')")
    @RequestMapping(value = "/generic_assay_data/{molecularProfileId}", method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Get all generic_assay_id in a molecular profile")
    public ResponseEntity<List<GenericAssayData>> getAllGenericAssayDataInMolecularProfile(
        @ApiParam(required = true, value = "Molecular Profile ID")
        @PathVariable String molecularProfileId,
        @ApiParam(required = true, value = "Sample List ID")
        @RequestParam String sampleListId,
        @ApiParam(required = true, value = "Generic Assay Stable ID")
        @RequestParam String genericAssayStableId,
        @ApiParam("Level of detail of the response")
        @RequestParam(defaultValue = "SUMMARY") Projection projection) throws MolecularProfileNotFoundException {

        List<GenericAssayData> result = genericAssayService.getGenericAssayData(
            molecularProfileId, sampleListId, Arrays.asList(genericAssayStableId), projection.name());

        if (projection == Projection.META) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(HeaderKeyConstants.TOTAL_COUNT, String.valueOf(result.size()));
            return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }

    @PreAuthorize("hasPermission(#molecularProfileId, 'MolecularProfileId', 'read')")
    @RequestMapping(value = "/generic_assay_data/{molecularProfileId}/fetch",
        method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("fetch all generic_assay_data in a molecular profile")
    public ResponseEntity<List<GenericAssayData>> fetchAllGenericAssayDataInMolecularProfile(
        @ApiParam(required = true, value = "Molecular Profile ID")
        @PathVariable String molecularProfileId,
        @ApiParam(required = true, value = "List of Sample IDs/Sample List ID and Entrez Gene IDs")
        @Valid @RequestBody GenericAssayDataFilter genericAssayDataFilter, 
        @ApiParam("Level of detail of the response")
        @RequestParam(defaultValue = "SUMMARY") Projection projection) throws MolecularProfileNotFoundException {

        List<GenericAssayData> result;
        if (genericAssayDataFilter.getSampleListId() != null) {
            result = genericAssayService.getGenericAssayData(molecularProfileId,
                genericAssayDataFilter.getSampleListId(), genericAssayDataFilter.getGenericAssayStableIds(), projection.name());
        } else {
            result = genericAssayService.fetchGenericAssayData(molecularProfileId,
                genericAssayDataFilter.getSampleIds(), genericAssayDataFilter.getGenericAssayStableIds(), projection.name());
        }

        if (projection == Projection.META) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(HeaderKeyConstants.TOTAL_COUNT, String.valueOf(result.size()));
            return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }

    @PreAuthorize("hasPermission(#involvedCancerStudies, 'Collection<CancerStudyId>', 'read')")
    @RequestMapping(value = "/generic_assay_data/fetch", method = RequestMethod.POST,
    consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation("Fetch generic_assay_data")
    public ResponseEntity<List<GenericAssayData>> fetchGenericAssayDataInMultipleMolecularProfiles(
        @ApiIgnore // prevent reference to this attribute in the swagger-ui interface
        @RequestAttribute(required = false, value = "involvedCancerStudies") Collection<String> involvedCancerStudies,
        @ApiIgnore // prevent reference to this attribute in the swagger-ui interface. this attribute is needed for the @PreAuthorize tag above.
        @RequestAttribute(required = false, value = "interceptedGenericAssayDataMultipleStudyFilter") GenericAssayDataMultipleStudyFilter interceptedGenericAssayDataMultipleStudyFilter,
        @ApiParam(required = true, value = "List of Molecular Profile ID and Sample ID pairs or List of Molecular" +
            "Profile IDs and Generic Assay IDs")
        @Valid @RequestBody(required = false) GenericAssayDataMultipleStudyFilter genericAssayDataMultipleStudyFilter,
        @ApiParam("Level of detail of the response")
        @RequestParam(defaultValue = "SUMMARY") Projection projection) throws MolecularProfileNotFoundException {

        List<GenericAssayData> result;
        if (interceptedGenericAssayDataMultipleStudyFilter.getMolecularProfileIds() != null) {
            result = genericAssayService.getGenericAssayDataInMultipleMolecularProfiles(
                interceptedGenericAssayDataMultipleStudyFilter.getMolecularProfileIds(), null,
                interceptedGenericAssayDataMultipleStudyFilter.getGenericAssayStableIds(), projection.name());
        } else {

            List<String> molecularProfileIds = new ArrayList<>();
            List<String> sampleIds = new ArrayList<>();
            extractMolecularProfileAndSampleIds(interceptedGenericAssayDataMultipleStudyFilter, molecularProfileIds, sampleIds);
            result = genericAssayService.getGenericAssayDataInMultipleMolecularProfiles(molecularProfileIds,
                sampleIds, interceptedGenericAssayDataMultipleStudyFilter.getGenericAssayStableIds(), projection.name());
        }

        if (projection == Projection.META) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(HeaderKeyConstants.TOTAL_COUNT, String.valueOf(result.size()));
            return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(result, HttpStatus.OK);
        }
    }

    private void extractMolecularProfileAndSampleIds(GenericAssayDataMultipleStudyFilter molecularDataMultipleStudyFilter, List<String> molecularProfileIds, List<String> sampleIds) {
        for (SampleMolecularIdentifier sampleMolecularIdentifier : molecularDataMultipleStudyFilter.getSampleMolecularIdentifiers()) {
            molecularProfileIds.add(sampleMolecularIdentifier.getMolecularProfileId());
            sampleIds.add(sampleMolecularIdentifier.getSampleId());
        }
    }
}
