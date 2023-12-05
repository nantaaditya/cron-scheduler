package com.nantaaditya.cronscheduler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nantaaditya.cronscheduler.entity.ClientRequest;
import com.nantaaditya.cronscheduler.model.response.ClientResponseDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CopyUtilTest {

  @Test
  void copy() {
    ClientRequest clientRequest = null;
    assertNull(CopyUtil.copy(clientRequest, new ClientResponseDTO()));

    clientRequest = new ClientRequest();
    clientRequest.setId("id");

    assertNotNull(CopyUtil.copy(clientRequest, new ClientResponseDTO()));
  }

  @Test
  void copySupplier() {
    ClientRequest clientRequest = null;
    assertNull(CopyUtil.copy(clientRequest, ClientResponseDTO::new));
  }

  @Test
  void copyList() {
    ClientRequest clientRequest = null;
    List<ClientRequest> clientRequests = null;
    assertEquals(Collections.emptyList(), CopyUtil.copy(clientRequests, ClientResponseDTO::new));

    clientRequests = new ArrayList<>();
    assertEquals(Collections.emptyList(), CopyUtil.copy(clientRequests, ClientResponseDTO::new));

    clientRequest = new ClientRequest();
    clientRequest.setId("id");
    clientRequests.add(clientRequest);
    assertNotNull(CopyUtil.copy(clientRequests, ClientResponseDTO::new));
  }

  @Test
  void copyListBiFunction() {
    ClientRequest clientRequest = null;
    List<ClientRequest> clientRequests = null;
    assertEquals(Collections.emptyList(), CopyUtil.copy(clientRequests, ClientResponseDTO::new, null));
  }
}