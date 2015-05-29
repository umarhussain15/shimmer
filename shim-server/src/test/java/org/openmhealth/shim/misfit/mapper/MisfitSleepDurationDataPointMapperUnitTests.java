package org.openmhealth.shim.misfit.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.openmhealth.schema.domain.omh.*;
import org.openmhealth.shim.common.mapper.DataPointMapperUnitTests;
import org.openmhealth.shim.common.mapper.JsonNodeMappingException;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.openmhealth.schema.domain.omh.DataPointModality.SENSED;
import static org.openmhealth.schema.domain.omh.DurationUnit.SECOND;
import static org.openmhealth.shim.misfit.mapper.MisfitDataPointMapper.RESOURCE_API_SOURCE_NAME;


/**
 * @author Emerson Farrugia
 */
public class MisfitSleepDurationDataPointMapperUnitTests extends DataPointMapperUnitTests {

    private final MisfitSleepDurationDataPointMapper mapper = new MisfitSleepDurationDataPointMapper();

    private JsonNode responseNode;

    @BeforeTest
    public void initializeResponseNode() throws IOException {

        ClassPathResource resource =
                new ClassPathResource("org/openmhealth/shim/misfit/mapper/misfit-sleeps.json");
        responseNode = objectMapper.readTree(resource.getInputStream());
    }

    @Test(expectedExceptions = JsonNodeMappingException.class)
    public void asDataPointsShouldThrowExceptionOnEmptySleepDetails() throws IOException {

        JsonNode node = objectMapper.readTree("{\n" +
                "    \"sleeps\": [\n" +
                "        {\n" +
                "            \"id\": \"54fa13a8440f705a7406845f\",\n" +
                "            \"autoDetected\": false,\n" +
                "            \"startTime\": \"2015-02-24T21:40:59-05:00\",\n" +
                "            \"duration\": 2580,\n" +
                "            \"sleepDetails\": []\n" +
                "        }\n" +
                "    ]\n" +
                "}");

        mapper.asDataPoints(singletonList(node));
    }

    @Test
    public void asDataPointsShouldReturnEmptyListIfAwake() throws IOException {

        JsonNode node = objectMapper.readTree("{\n" +
                "    \"sleeps\": [\n" +
                "        {\n" +
                "            \"id\": \"54fa13a8440f705a7406845f\",\n" +
                "            \"autoDetected\": false,\n" +
                "            \"startTime\": \"2015-02-24T21:40:59-05:00\",\n" +
                "            \"duration\": 2580,\n" +
                "            \"sleepDetails\": [\n" +
                "                {\n" +
                "                    \"datetime\": \"2015-02-24T21:40:59-05:00\",\n" +
                "                    \"value\": 1\n" +
                "                }\n" +
                "            ]\n" +
                "        }\n" +
                "    ]\n" +
                "}");

        List<DataPoint<SleepDuration>> dataPoints = mapper.asDataPoints(singletonList(node));

        assertThat(dataPoints, notNullValue());
        assertThat(dataPoints, empty());
    }

    @Test
    public void asDataPointsShouldReturnCorrectNumberOfDataPoints() {

        List<DataPoint<SleepDuration>> dataPoints = mapper.asDataPoints(singletonList(responseNode));

        assertThat(dataPoints, notNullValue());
        assertThat(dataPoints.size(), equalTo(2));
    }

    @Test
    public void asDataPointsShouldReturnCorrectDataPoints() {

        List<DataPoint<SleepDuration>> dataPoints = mapper.asDataPoints(singletonList(responseNode));

        assertThat(dataPoints, notNullValue());
        assertThat(dataPoints.size(), greaterThan(0));

        // the end time is the addition of the start time and the total duration
        TimeInterval effectiveTimeInterval = TimeInterval.ofStartDateTimeAndEndDateTime(
                OffsetDateTime.of(2015, 2, 23, 21, 40, 59, 0, ZoneOffset.ofHours(-5)),
                OffsetDateTime.of(2015, 2, 24, 0, 53, 59, 0, ZoneOffset.ofHours(-5)));

        // the sleep duration is the total duration minus the sum of the awake segment durations
        SleepDuration sleepDuration = new SleepDuration.Builder(new DurationUnitValue(SECOND, 10140))
                .setEffectiveTimeFrame(effectiveTimeInterval)
                .build();

        DataPoint<SleepDuration> firstDataPoint = dataPoints.get(0);

        assertThat(firstDataPoint.getBody(), equalTo(sleepDuration));

        DataPointAcquisitionProvenance acquisitionProvenance = firstDataPoint.getHeader().getAcquisitionProvenance();

        assertThat(acquisitionProvenance, notNullValue());
        assertThat(acquisitionProvenance.getSourceName(), equalTo(RESOURCE_API_SOURCE_NAME));
        assertThat(acquisitionProvenance.getModality(), equalTo(SENSED));
    }
}