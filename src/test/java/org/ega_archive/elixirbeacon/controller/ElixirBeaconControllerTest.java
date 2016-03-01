package org.ega_archive.elixirbeacon.controller;

import org.ega_archive.elixirbeacon.Application;
import org.ega_archive.elixirbeacon.controller.ElixirBeaconController;
import org.ega_archive.elixirbeacon.dto.Beacon;
import org.ega_archive.elixirbeacon.dto.BeaconAlleleResponse;
import org.ega_archive.elixirbeacon.service.ElixirBeaconService;
import org.ega_archive.elixircore.constant.ParamName;
import org.ega_archive.elixircore.helper.CommonQuery;
import org.ega_archive.elixircore.test.util.TestUtils;
import org.ega_archive.elixircore.util.JsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.notNullValue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
public class ElixirBeaconControllerTest {

  @Autowired
  @InjectMocks
  private ElixirBeaconController controller;
  
  @Mock
  private ElixirBeaconService elixirBeaconServiceMock;
  
  private MockMvc mockMvc;
  
  @Autowired
  private ObjectMapper objectMapper;
  
  @Autowired
  private WebApplicationContext wac;
  
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    
    mockMvc = MockMvcBuilders.webAppContextSetup(wac)
        .alwaysExpect(status().isOk())
        .alwaysExpect(content().contentType(TestUtils.APPLICATION_JSON_CHARSET_UTF_8))
        .build();
  }

  @Test
  public void callInfo() throws Exception {
    when(
        elixirBeaconServiceMock.listDatasets(Mockito.any(CommonQuery.class), Mockito.anyString())).thenReturn(new Beacon());
    
    MvcResult mvcResult = mockMvc.perform(get("/beacon/info")
        .accept(MediaType.APPLICATION_JSON))
        .andReturn();
    
    Beacon response = JsonUtils.jsonToObject(mvcResult.getResponse().getContentAsString(), Beacon.class, objectMapper);
    
    assertThat(response, notNullValue(Beacon.class));
  }
  
  @Test
  public void callRoot() throws Exception {
    when(
        elixirBeaconServiceMock.listDatasets(Mockito.any(CommonQuery.class), Mockito.anyString())).thenReturn(new Beacon());
    
    MvcResult mvcResult = mockMvc.perform(get("/beacon/")
        .accept(MediaType.APPLICATION_JSON))
        .andReturn();
    
    Beacon response = JsonUtils.jsonToObject(mvcResult.getResponse().getContentAsString(), Beacon.class, objectMapper);
    
    assertThat(response, notNullValue(Beacon.class));
  }
  
  @Test
  public void callQuery() throws Exception {
    when(
        elixirBeaconServiceMock.queryBeacon(Mockito.anyListOf(String.class), Mockito.anyString(),
            Mockito.isNull(String.class), Mockito.anyString(), Mockito.any(Integer.class),
            Mockito.anyString())).thenReturn(new BeaconAlleleResponse());
    
    MvcResult mvcResult = mockMvc.perform(get("/beacon/query")
        .param(ParamName.BEACON_ALTERNATE_BASES, "A")
        .param(ParamName.BEACON_CHROMOSOME, "19")
        .param(ParamName.BEACON_DATASET_IDS, "EGAD00000000029")
        .param(ParamName.BEACON_POSITION, "1234")
        .param(ParamName.BEACON_REFERENCE_GENOME, "grch37")
        .accept(MediaType.APPLICATION_JSON))
        .andReturn();
    
    BeaconAlleleResponse response = JsonUtils.jsonToObject(mvcResult.getResponse().getContentAsString(), BeaconAlleleResponse.class, objectMapper);
    
    assertThat(response, notNullValue(BeaconAlleleResponse.class));
    
    mvcResult = mockMvc.perform(post("/beacon/query")
        .param(ParamName.BEACON_CHROMOSOME, "19")
        .param(ParamName.BEACON_POSITION, "1234")
        .param(ParamName.BEACON_REFERENCE_GENOME, "grch37")
        .accept(MediaType.APPLICATION_JSON))
        .andReturn();
    
    response = JsonUtils.jsonToObject(mvcResult.getResponse().getContentAsString(), BeaconAlleleResponse.class, objectMapper);
    
    assertThat(response, notNullValue(BeaconAlleleResponse.class));
  }
  
  @Test
  public void callAlleles() throws Exception {
    when(
        elixirBeaconServiceMock.queryBeacon(Mockito.anyListOf(String.class), Mockito.anyString(),
            Mockito.isNull(String.class), Mockito.anyString(), Mockito.any(Integer.class),
            Mockito.anyString())).thenReturn(new BeaconAlleleResponse());
    
    MvcResult mvcResult = mockMvc.perform(get("/beacon/alleles")
        .param(ParamName.BEACON_ALT_BASES, "A")
        .param(ParamName.BEACON_CHROM, "19")
        .param(ParamName.BEACON_DATASET_IDS, "EGAD00000000029")
        .param(ParamName.BEACON_DATASET_IDS, "EGAD00000000028")
        .param(ParamName.BEACON_POS, "1234")
        .param(ParamName.BEACON_REFERENCE_GENOME, "grch37")
        .accept(MediaType.APPLICATION_JSON))
        .andReturn();
    
    BeaconAlleleResponse response = JsonUtils.jsonToObject(mvcResult.getResponse().getContentAsString(), BeaconAlleleResponse.class, objectMapper);

    assertThat(response, notNullValue(BeaconAlleleResponse.class));
  }
  
}
